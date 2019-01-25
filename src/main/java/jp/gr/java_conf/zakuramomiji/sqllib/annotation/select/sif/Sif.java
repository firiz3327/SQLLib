/*
 * Sif.java
 * 
 * Copyright (c) 2019 firiz.
 * 
 * This file is part of Expression program is undefined on line 6, column 40 in Templates/Licenses/license-licence-gplv3.txt..
 * 
 * Expression program is undefined on line 8, column 19 in Templates/Licenses/license-licence-gplv3.txt. is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Expression program is undefined on line 13, column 19 in Templates/Licenses/license-licence-gplv3.txt. is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Expression program is undefined on line 19, column 30 in Templates/Licenses/license-licence-gplv3.txt..  If not, see <http ://www.gnu.org/licenses/>.
 */
package jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.sif;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author firiz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sif {

    public enum SifType {
        EQUALS("="), // 等しい
        NOT_EQUALS("!="), // 等しくない
        MORE(">="), // 以上
        MORE_THAN(">"), // より大きい
        LESS("<="), // 以下
        LESS_THAN("<"), // より小さい
        ;

        private final String word;

        private SifType(String word) {
            this.word = word;
        }

        public String getWord() {
            return word;
        }
    }
    
    public enum SifPipe {
        AND,
        OR
    }

    public SifType type();

    public String[] value() default "";
    
    public SifPipe pipe() default SifPipe.AND;

}
