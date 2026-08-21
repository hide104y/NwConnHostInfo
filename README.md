# NwConnHostInfo

## ビルドツールについて
- 2026/08現在：Java 1.7で、「apache-maven-3.8.9」がプラグイン取得時にエラーとなるため、「apache-ant-1.9.16」を利用する

## 事前作業
1. JDKがインストールされていない場合はインストール
1. Github CLIがインストールされていない場合はインストール：winget install -e --id GitHub.cli
1. Powershellプロンプトを開く

## リポジトリ作成（未作成の場合）
```shell
# サインイン状態の確認
gh auth status
# 初回サインインしていない場合はサインイン
gh auth login
# 削除権限付与
gh auth refresh -h github.com -s delete_repo
# 作成
gh repo create NwConnHostInfo --private
# 確認
gh repo list | Select-String NwConnHostInfo
```

## リモートリポジトリ（mainブランチ）の取得
```shell
# CD
cd D:\Github\workspace.jre7
# フォルダが存在する場合は削除
if (-Not (Test-Path -Path .\NwConnHostInfo)){rmdir .\NwConnHostInfo}
# クローン実行
git clone https://github.com/hide104y/NwConnHostInfo.git
```

## リモートリポジトリ（mainブランチ）にREADME.mdが存在しない場合
```shell
# CD
cd D:\Github\workspace.jre7\NwConnHostInfo
# ファイル作成
ruby -e "File.write('README.md', '# NwConnHostInfo', encoding: 'UTF-8')"
# コミット
git add README.md
git commit -m "add README.md"
# プッシュ
git push -u origin main
# ブランチの一覧表示
git branch -a
```

## ブランチの作成
```shell
# ブランチをmainに切り替え・復元
git checkout main
# ブランチ作成
git checkout -b java07
# 作成したブランチをリモートにプッシュ
git push -u origin java07
```

## Java、Antの切り替え
```shell
# PATHの設定
$Env:JAVA_HOME="${Env:USERPROFILE}\App\Java\jdk1.7.0_80"
$Env:ANT_HOME="${Env:USERPROFILE}\App\Ant\apache-ant-1.9.16"
$Env:PATH="${Env:JAVA_HOME}\bin;${Env:ANT_HOME}\bin;${Env:PATH}"
# 確認
java -version
ant -version
```

## ディレクトリ作成
```shell
mkdir src\main\java
mkdir src\test\java
mkdir src\main\resources
mkdir src\main\java\org\apache\commons\net\util
```

## 手動配置が必要な依存ライブラリーソースの配置
- src\main\java\org\apache\commons\net\util\SubnetUtils.java

## 手動配置が必要な依存ライブラリーの配置
- src\main\resources\commons-exec-1.3.jar

## コーディング
- build.xml
- src\main\java\tool\NwConnHostInfo.java
- src\main\java\tool\clsAppProp.java
- src\main\java\tool\clsCmdExec.java
- src\main\java\tool\clsCmdStdOut.java
- src\main\java\tool\clsNetstat.java
- src\main\java\tool\clsPipeParser.java
- src\main\java\tool\clsProperties.java

## AIレビュー
```shell
# CD
cd D:\Github\workspace.jre7
agy
.\NwConnHostInfo\src配下のソースに対して、スキル「source-review」を実行して
/exit
```

## ビルド
```shell
# CD
cd D:\Github\workspace.jre7\NwConnHostInfo
# クリーン
ant clean
# 単体テスト
ant test
# ビルド
ant
# Usage
java -jar target\NwConnHostInfo-1.0-jre7.jar -h
# 動作確認
java -jar target\NwConnHostInfo-1.0-jre7.jar -c 3 -s 3 --pid -vv
java -jar target\NwConnHostInfo-1.0-jre7.jar -c 3 -s 3 --pid -vv -o D:\Github\workspace.jre7\NwConnHostInfo\target
```

## リポジトリにコミット
```shell
# CD
cd D:\Github\workspace.jre7\NwConnHostInfo
# ブランチをjava07に切り替え
git switch java07
# コミット
git add .
git commit -m "Gemini 3.6 Flash (High) Review & Modified"
# リモートリポジトリ（java07ブランチ）にプッシュ
git push -u origin java07
```

## リモートリポジトリを確認
- https://github.com/hide104y/NwConnHostInfo/tree/java07
<br>※GitHubの画面で「Compare & pull request」が表示されるが放置

## リモートリポジトリ（java07ブランチ）の取得
```shell
# CD
cd D:\Github\workspace.jre7
# フォルダが存在する場合は削除
if (-Not (Test-Path -Path .\NwConnHostInfo)){rmdir .\NwConnHostInfo}
# クローン実行
git clone -b java07 https://github.com/hide104y/NwConnHostInfo.git
```

## License
- These codes are licensed under CC0.
- http://creativecommons.org/publicdomain/zero/1.0/deed.ja
