package com.zhranklin

import java.io.{File, FileWriter}
import java.util.stream.{Collectors, Stream => JStream}
import javax.swing.text.html.StyleSheet.ListPainter

import com.google.gson.GsonBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Node}
import org.jsoup.select.NodeVisitor

import scala.collection.mutable
import scala.language.postfixOps
import scala.sys.process._
import scala.util.{Random, Try}
import scala.collection.JavaConversions._

case class Question(content: String, answer: String, validity: String) {
  override def toString = {
    val v = validity == "正确"
    s"""Question:
       |$content#answer#: ${if (!v) "∈" else ""}$answer
       |
       |
       |""".stripMargin
  }
}

class QType() {
  override def toString: String = throw new NullPointerException
}

case class DX() extends QType {
  override def toString = "dx"
}

case class PD() extends QType {
  override def toString = "pd"
}


class Petest(xh: String, pwd: String) {
  val COOKIE_FILE = xh + ".txt"
  val LOGIN_UTL = "http://pead.scu.edu.cn/stu/dl.asp"
  val LX_URL = "http://pead.scu.edu.cn/stu/lllx.asp"
  val iconvCommand = Seq("iconv", "-t", "UTF-8", "-f", "GBK")
  val judgePattern = "正确|错误".r

  def login() = Seq("curl", "-d", s"xh=$xh&pwd=$pwd", "-c", COOKIE_FILE, LOGIN_UTL) !

  def getBody = Jsoup.parse(Seq("curl", "-b", COOKIE_FILE, LX_URL) #| iconvCommand !!).body

  def getQuestionElement(body: Element) = body.select("span[class=style7]").first.parent

  def getQuestionType(body: Element): QType = {
    if (body.select("input[name=dx][type=hidden]").first != null) DX()
    else if (body.select("input[name=pd][type=hidden]").first != null) PD()
    else new QType
  }

  def choose(question: Element, qType: QType): String = {
    val answers = new mutable.MutableList[String]
    question.select(s"input[name=${qType}r]").traverse(new NodeVisitor() {
      def head(node: Node, depth: Int) =
        if (depth == 0)
          answers += node.attr("value")

      def tail(node: Node, depth: Int) = {}
    })
    answers.get(Random.nextInt(answers.length)).get
  }

  def validate(id: String, ans: String, qType: QType) =
    judgePattern
      .findFirstIn(Seq("curl", "-b", COOKIE_FILE, s"$LX_URL?$qType=$id&${qType}r=$ans") #| iconvCommand !!)
      .getOrElse("无效")

  def getQuestionFromBody(body: Element, qType: QType) = {
    val id = body.select(s"input[name=$qType][type=hidden]").first.attr("value")
    val qElement = getQuestionElement(body)
    val answer = choose(qElement, qType)
    val validity = validate(id, answer, qType)
    val content =
      qElement.html
        .replaceAll("<br>", "\n")
        .replaceAll("\n", "#nl#")
        .replaceAll("<.*?>", " ")
        .replaceAll("\\s\\s+", " ")
        .replaceAll("\\s*#nl#", "\n")
    Question(content, answer, validity)
  }

  def getQuestion = {
    val body = getBody
    getQuestionFromBody(body, getQuestionType(body))
  }
}

object Petest {
  val FILE_NAME = "output.json"
  val gson = new GsonBuilder().create

  def fetch(args: Array[String]): Unit = {
    val petest = new Petest(args(1), args(2))
    val times = Integer.parseInt(args(3))
    val interval = if (args.length > 4) Integer.parseInt(args(4)) else 10000
    val fw = new FileWriter(FILE_NAME, true)
    petest.login()
    petest.getBody
    1 to times foreach { i =>
      print(s"第${i}次尝试...")
      try {
        val q = petest.getQuestion
        gson.toJson(q, fw)
        fw.append('\n')
        fw.flush()
        println("成功.")
      } catch {
        case _: Exception => println("失败.")
      }
      Thread.sleep(interval)
    }
    fw.close()
  }

  val HELP =
    """第一个参数为fetch或merge。
      |
    """.stripMargin

  def main(args: Array[String]): Unit = args(0) match {
    case "fetch" => fetch(args)
    case "merge" => printMergedData()
    case _ => println(HELP)
  }

  def merge() = {
    Console.in.lines
      .filter(_.startsWith("{"))
      .map[Question] { l => 
        try {
          gson.fromJson(l, classOf[Question])
        } catch {
          case e:Exception => Question("", "", "无效")
        }
      }
      .collect(Collectors.groupingBy[Question, String](_.content))
      .map(_._2)
      .map { sameQuestions =>
        sameQuestions.find(_.validity == "正确")
          .getOrElse {
            val content = sameQuestions.head.content
            val dx = "单选题".r.findFirstIn(content).nonEmpty
            val wa =
              sameQuestions
                .filter(_.validity == "错误")
                .groupBy(_.answer.head)
                .keys
                .toList
            val difference = (if (dx) 'A' to 'D' else 'A' to 'B').toList.diff(wa)
            val validity = if (difference.length == 1) "正确" else "可能"
            Question(content, difference.mkString(""), validity)
          }
      }
  }

  def printMergedData() = {
    val data = merge()
    val str = data.mkString("")
    println(s"总计: ${data.size}\n$str")
  }
}
