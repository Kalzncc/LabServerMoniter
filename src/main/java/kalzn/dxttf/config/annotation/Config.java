package kalzn.dxttf.config.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
    boolean injectFlag() default true;
    String key() default "";
}

