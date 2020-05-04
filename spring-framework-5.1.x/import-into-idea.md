The following has been tested against IntelliJ IDEA 2016.2.2

## Steps

step1：需要jdk1.8以上，但是版本不能太高1.8.121左右，80或者192版本编译均会出现错误

step2：下载配置Gradle，并在idea中配置好自动导包和gradle的安装路径和保存jar包路径

step3：index索引完成以后，首先执行beans、context、core、oxm下面的test方法，进行编译

step4：编译时aspects项目可能会出现问题，在使用spring期间，大可排除aspects项目，以避免编译报错

提示：
    1、出现strategy.InstantiatorStrategy..等class文件不存在的情况
        更新idea的kotlin插件
    2、再次运行测试失败
        需要cmd在spring-core所在目录下，执行
        gradle objenesisRepackJar
        gradle cglibRepackJar

​	3、刷新依赖  刷新依赖  刷新依赖


Within your locally cloned spring-framework working directory:_

1. Precompile `spring-oxm` with `./gradlew :spring-oxm:compileTestJava`
2. Import into IntelliJ (File -> New -> Project from Existing Sources -> Navigate to directory -> Select build.gradle)
3. When prompted exclude the `spring-aspects` module (or after the import via File-> Project Structure -> Modules)
4. Code away

## Known issues

1. `spring-core` and `spring-oxm` should be pre-compiled due to repackaged dependencies.
See `*RepackJar` tasks in the build and https://youtrack.jetbrains.com/issue/IDEA-160605).
2. `spring-aspects` does not compile due to references to aspect types unknown to
IntelliJ IDEA. See https://youtrack.jetbrains.com/issue/IDEA-64446 for details. In the meantime, the
'spring-aspects' can be excluded from the project to avoid compilation errors.
3. While JUnit tests pass from the command line with Gradle, some may fail when run from
IntelliJ IDEA. Resolving this is a work in progress. If attempting to run all JUnit tests from within
IntelliJ IDEA, you will likely need to set the following VM options to avoid out of memory errors:
    -XX:MaxPermSize=2048m -Xmx2048m -XX:MaxHeapSize=2048m
4. If you invoke "Rebuild Project" in the IDE, you'll have to generate some test
resources of the `spring-oxm` module again (`./gradlew :spring-oxm:compileTestJava`)    


## Tips

In any case, please do not check in your own generated .iml, .ipr, or .iws files.
You'll notice these files are already intentionally in .gitignore. The same policy goes for eclipse metadata.

## FAQ

Q. What about IntelliJ IDEA's own [Gradle support](https://confluence.jetbrains.net/display/IDEADEV/Gradle+integration)?

A. Keep an eye on https://youtrack.jetbrains.com/issue/IDEA-53476
