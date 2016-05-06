# pe-test

## Description
川大体育学院抓题脚本

## Disclaimer
- 该工具没有做任何错误处理, 所以小心使用。
- 仅供娱乐, 不要干坏事哟...

## Prerequisites
- 要求系统带有curl和iconv两个命令(mac OSX下需要自行安装)
- 安装[sbt](http://www.scala-sbt.org/0.13/docs/zh-cn/Setup.html)

## Build
- 安装sbt并确保curl和iconv可以使用
- `cd /the/root/path`
- `sbt assembly`

然后sbt就会自动下载所有依赖, 如果顺利的话, 会将jar文件打包到:target/scala-2.11/petest.jar

## Usage
### fetch
```bash
java -jar petest.jar fetch 学号 密码 条数 [间隔(ms)] 2> /dev/null
```

间隔如果不填写则默认为10000(10s), 如果数据量比较大, 一定不要改小了, 不然被查水表后果自负..

该工具会生成两个文件, 一个是学号.txt(临时使用), 里面包含了你的cookie信息, 还有一个是output.json, 里面按照json格式输出了题目对象:

```json
{
  "content": "题目内容",
  "answer": "A/B/C/D",
  "validity": "正确/错误/无效"
}
```

这个工具是直接蒙题的, 所以validity表示这个答案是不是正确(如果出现了"无效"说明出问题了, 请忽略), 当然了这样肉眼看题有点麻烦, 相关的分析工具会在之后完成。

在目前没有分析工具的情况下, 可以使用sort命令排序, 这样相同的题目就会排在一起了:

```bash
sort output.json > sorted.json
```

其实sorted.json就足够使用了

可以放在服务器上慢慢跑...注意, 是慢慢跑, 慢慢的....

如果要挂载服务器上, 可以使用nohup命令:

```bash
nohup <your command> &
```

这样就可以安安心心退出ssh了。

### merge
现已添加merge命令, 用来处理json:

```
java -jar petest.jar merge < output.json > result..txt
```

