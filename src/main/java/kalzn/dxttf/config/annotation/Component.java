package kalzn.dxttf.config.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    int COMPONENT = 0;
    int DATABASE  = 1;
    int SERVICE   = 2;
    int ROUTER    = 3;
    int EXECUTOR  = 4;

    boolean disable() default false;
    String[] name() default {};
    int type() default COMPONENT;
}
