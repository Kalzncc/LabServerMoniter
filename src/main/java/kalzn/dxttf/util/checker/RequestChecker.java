package kalzn.dxttf.util.checker;

import kalzn.dxttf.config.GlobalConfig;

import java.util.Map;

public class RequestChecker {

    public static interface Constraint {
        boolean check(Object value);
    }
    public static class Remember {
        private String name;
        private String clazz;
        private Constraint constraint;
        public Remember(String name, String clazz, Constraint constraint) {
            this.name = name;
            this.clazz = clazz;
            this.constraint = constraint;
        }
        public Remember(String name, String clazz) {
            this.name = name;
            this.clazz = clazz;
        }
    }

    public static boolean checkContain(Map<String, Object> req, Remember[] template, boolean strictCheck) {
        for (var remember : template) {
            if (!req.containsKey(remember.name)) return false;
            Object value = req.get(remember.name);
            if (!remember.clazz.equals(value.getClass().getName())) {
                return false;
            }

            if (remember.constraint != null && !remember.constraint.check(value)) return false;
        }

        return req.size() == template.length || !strictCheck;
    }

    public static boolean checkLoginByPwdReq(Map<String, Object> req) {
        Remember[] template = {
            new Remember("name", String.class.getName(),
                    (val) -> ((String)val).matches(GlobalConfig.auth.namePatten)),
            new Remember("password", String.class.getName())
        };
        return checkContain(
                req,
                template,
                true
        );
    }
}
