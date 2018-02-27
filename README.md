# plantuml-server
plantuml を Web でドキュメント変換させるやつ/ WebFlux でやってるけど、ブロッキングしてるので、ちょっとあれ

使い方
===

サーバー起動して

```sh
./gradlew bootRun
```

`http://localhost:8080/uml/{base64エンコードしたplantumlファイル}` にアクセスすると svg が返ってきます

```sh
echo $(openssl base64 -in uml.txt | tr -d '\n') | \
  awk '{print "http://localhost:8080/uml"$1}' | \
  xargs curl -v
```

必要な環境
===

* Java8
* Graphviz
