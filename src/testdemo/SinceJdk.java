package testdemo;

/**
 * 该方法默认啥时候添加
 */
public @interface SinceJdk {

    String version() default "1.8";
}
