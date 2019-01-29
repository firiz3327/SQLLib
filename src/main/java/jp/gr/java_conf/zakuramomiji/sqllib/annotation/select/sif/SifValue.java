/*
 * SifValue.java
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

import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.sif.Sif.SifPipe;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.sif.Sif.SifType;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author firiz
 */
public class SifValue {

    private final String column;
    private final SifType type;
    private final SifPipe pipe;
    private final String[] value;

    public SifValue(@NotNull String column, @NotNull SifType type, @NotNull String value) {
        this(column, type, SifPipe.AND, new String[]{value});
    }

    public SifValue(@NotNull String column, @NotNull SifType type, @NotNull SifPipe pipe, @NotNull String value) {
        this(column, type, pipe, new String[]{value});
    }

    public SifValue(@NotNull String column, @NotNull SifType type, @NotNull String[] value) {
        this(column, type, SifPipe.AND, value);
    }

    public SifValue(@NotNull String column, @NotNull SifType type, @NotNull SifPipe pipe, @NotNull String[] value) {
        this.column = column;
        this.type = type;
        this.pipe = pipe;
        this.value = value;
    }

    @NotNull
    public String getColumn() {
        return column;
    }

    @NotNull
    public SifType getType() {
        return type;
    }

    @NotNull
    public String[] getValue() {
        return value;
    }

    @NotNull
    public SifPipe getPipe() {
        return pipe;
    }

}
