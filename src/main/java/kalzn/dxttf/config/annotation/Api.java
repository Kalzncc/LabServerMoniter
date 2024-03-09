package kalzn.dxttf.config.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Api {

    boolean disable() default false;
    String[] types() default {"get"};
    String mapping () default "";

    int priority() default 0;
}
