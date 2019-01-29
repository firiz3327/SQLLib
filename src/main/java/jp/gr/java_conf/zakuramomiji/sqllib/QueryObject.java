/*
 * QueryObject.java
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
package jp.gr.java_conf.zakuramomiji.sqllib;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import jp.gr.java_conf.zakuramomiji.sqllib.SQLLib;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.Table;

/**
 *
 * @author firiz
 */
public abstract class QueryObject implements Cloneable {

    private String table;

    public QueryObject() {
        this.table = null;
    }

    public QueryObject(String table) {
        this.table = table;
    }

    public final String getTable() {
        if (table == null) {
            final Table annotation = getClass().getAnnotation(Table.class);
            if (annotation != null) {
                return annotation.value();
            }
            throw new IllegalArgumentException("Please set a table for class or variable.");
        }
        return table;
    }

    public String getTableUpper() {
        if (table == null) {
            final Table annotation = getClass().getAnnotation(Table.class);
            if (annotation != null) {
                return annotation.value().toUpperCase();
            }
            throw new IllegalArgumentException("Please set a table for class or variable.");
        }
        return table.toUpperCase();
    }

    public List<Object> getFieldValues() {
        final List<Object> result = new ArrayList<>();
        try {
            final Field[] fields = this.getClass().getDeclaredFields();
            for (final Field f : fields) {
                f.setAccessible(true);
                result.add(f.get(this));
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            SQLLib.INSTANCE.log(ex);
        }
        return result;
    }

    public Object get(final String name) {
        try {
            final Field field = this.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(this);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            SQLLib.INSTANCE.log(ex);
        }
        return null;
    }

    public boolean getBoolean(final String name) {
        return (boolean) get(name);
    }

    public int getByte(final String name) {
        return (byte) get(name);
    }

    public short getShort(final String name) {
        return (short) get(name);
    }

    public int getInt(final String name) {
        return (int) get(name);
    }

    public long getLong(final String name) {
        return (long) get(name);
    }

    public String getString(final String name) {
        return (String) get(name);
    }

    public void set(final String name, final Object value) {
        try {
            final Field field = this.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(this, value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            SQLLib.INSTANCE.log(ex);
        }
    }

    @Override
    public QueryObject clone() throws CloneNotSupportedException {
        QueryObject result = null;
        try {
            result = (QueryObject) super.clone();
            result.table = this.table;
        } catch (CloneNotSupportedException ex) {
            SQLLib.INSTANCE.log(ex);
        }
        return result;
    }

}
