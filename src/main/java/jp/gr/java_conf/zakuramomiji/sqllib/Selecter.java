/*
 * Selecter.java
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.Table;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.convert.Converter;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.Same;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.Select;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.TSame;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.sif.Sif;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.sif.SifValue;
import jp.gr.java_conf.zakuramomiji.sqllib.utils.DoubleData;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author firiz
 */
final class Selecter extends QueryRunner {

    public Selecter(SQLLib lib, Connection conn, boolean debug) {
        super(lib, conn, debug);
    }

    public <T extends QueryObject> List<T> select(@NotNull final T obj) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        return select(obj, null, null);
    }
    
    public <T extends QueryObject> List<T> select(@NotNull final T obj, final SifValue[] sifValues) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        return select(obj, sifValues, null);
    }

    private <T extends QueryObject> List<T> select(@NotNull final T obj, final SifValue[] sifValues, final QueryObject parent) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        final Map<String, Select> selects = new LinkedHashMap<>();
        final Map<String, Table> tables = new LinkedHashMap<>();
        final Map<String, Sif> sifs = new LinkedHashMap<>();
        final List<SifValue> sifvs = new ArrayList<>();
        final Map<String, Same[]> sames = new LinkedHashMap<>();
        final Map<String, DoubleData<Field, QueryObject>> lists = new LinkedHashMap<>();

        final Field[] fields = obj.getClass().getDeclaredFields();
        for (final Field f : fields) {
            f.setAccessible(true);
            final String name = f.getName();

            if (sifValues != null) {
                for (final SifValue sv : sifValues) {
                    if (name.equals(sv.getColumn())) {
                        sifvs.add(sv);
                        break;
                    }
                }
            }

            // is queryobject
            if (f.getType() == List.class) {
                final Class type = (Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                final Constructor c = type.isMemberClass()
                        ? type.getDeclaredConstructor(obj.getClass())
                        : type.getDeclaredConstructor();
                lists.put(name, new DoubleData<>(f, (QueryObject) (type.isMemberClass() ? c.newInstance(obj) : c.newInstance())));
            } else {
                // selects
                final Select select = f.getAnnotation(Select.class);
                if (select != null) {
                    selects.put(name, select);
                }
                // froms
                final Table table = f.getAnnotation(Table.class);
                if (table != null) {
                    tables.put(name, table);
                }
                // wheres
                final Sif sif = f.getAnnotation(Sif.class);
                if (sif != null) {
                    sifs.put(name, sif);
                }
                // sames
                final Same[] sameList = f.getAnnotationsByType(Same.class);
                if (sameList != null && sameList.length != 0) {
                    sames.put(name, sameList);
                }
            }
        }

        if (selects.isEmpty()) {
            throw new IllegalArgumentException("Select annotation can't be found.");
        }

        final StringBuilder query = new StringBuilder();
        query.append("SELECT ");

        // selects
        boolean first = true;
        for (final String column : selects.keySet()) {
            if (!lists.containsKey(column)) {
                query.append(first ? "" : ",")
                        .append(
                                tables.containsKey(column)
                                ? tables.get(column).value().toUpperCase()
                                : obj.getTableUpper()
                        )
                        .append(".")
                        .append(column);
                first = false;
            }
        }
        // froms
        query.append(" FROM ")
                .append(obj.getTable())
                .append(" ")
                .append(obj.getTableUpper());

        final List<String> uniqueCheck = new ArrayList<>();
        tables.keySet().stream().map((column) -> tables.get(column)).forEachOrdered((table) -> {
            final String upper = table.value().toUpperCase();
            if (!uniqueCheck.contains(upper)) {
                uniqueCheck.add(upper);
                query.append(",")
                        .append(table.value())
                        .append(" ")
                        .append(upper);
            }
        });
        // wheres
        final List<Object> whereValues = new ArrayList<>();
        final TSame tsame = obj.getClass().getAnnotation(TSame.class);
        if (!sifs.isEmpty() || !sifvs.isEmpty() || !sames.isEmpty() || tsame != null) {
            query.append(" WHERE ");

            first = true;
            if (tsame != null && parent != null) {
                first = false;
                whereValues.add(parent.get(tsame.origin()));
                query.append(obj.getTableUpper()).append(".").append(tsame.column()).append("=?");
            }

            // AテーブルのaaaカラムとBテーブルのbbbカラムの同一性を示す
            for (final String column : sames.keySet()) {
                final Same[] sameList = sames.get(column);
                for (final Same same : sameList) {
                    query.append(first ? "" : " AND ")
                            .append(
                                    tables.containsKey(column)
                                    ? tables.get(column).value().toUpperCase()
                                    : obj.getTableUpper()
                            ).append(".")
                            .append(column)
                            .append("=")
                            .append(same.table().toUpperCase())
                            .append(".")
                            .append(same.column());
                    first = false;
                }
            }

            first = true;
            // 条件を設定
            for (final SifValue sif : sifvs) {
                if (first) {
                    if (!sames.isEmpty()) {
                        query.append(" AND ");
                    }
                } else {
                    query.append(" ").append(sif.getPipe().toString()).append(" ");
                }
                query.append(
                        tables.containsKey(sif.getColumn())
                        ? tables.get(sif.getColumn()).value().toUpperCase()
                        : obj.getTableUpper()
                ).append(".").append(sif.getColumn()).append(sif.getType().getWord()).append("?");
                final String[] vals = sif.getValue();
                if (vals.length == 1) {
                    whereValues.add(vals[0]);
                } else {
                    final StringBuilder temp = new StringBuilder();
                    for (int i = 0; i < vals.length; i++) {
                        temp.append(i == 0 ? "" : ",").append(vals[i]);
                    }
                    whereValues.add(temp.toString());
                }
                first = false;
            }
            for (final String column : sifs.keySet()) {
                final Sif sif = sifs.get(column);
                if (first) {
                    if (!sames.isEmpty()) {
                        query.append(" AND ");
                    }
                } else {
                    query.append(" ").append(sif.pipe().toString()).append(" ");
                }
                query.append(
                        tables.containsKey(column)
                        ? tables.get(column).value().toUpperCase()
                        : obj.getTableUpper()
                ).append(".").append(column).append(sif.type().getWord()).append("?");
                final String[] vals = sif.value();
                if (vals.length == 1) {
                    whereValues.add(vals[0]);
                } else {
                    final StringBuilder temp = new StringBuilder();
                    for (int i = 0; i < vals.length; i++) {
                        temp.append(i == 0 ? "" : ",").append(vals[i]);
                    }
                    whereValues.add(temp.toString());
                }
                first = false;
            }
        }

        if (debug) {
            lib.info(query);
        }

        final List objs = new ArrayList();
        try (final PreparedStatement ps = conn.prepareStatement(query.toString())) {
            for (int i = 0; i < whereValues.size(); i++) {
                ps.setObject(i + 1, whereValues.get(i));
            }

            try (final ResultSet result = ps.executeQuery()) {
                first = true;
                while (result.next()) {
                    final QueryObject val;
                    if (first) {
                        first = false;
                        val = obj;
                    } else {
                        val = obj.clone();
                    }
                    for (final Field f : fields) {
                        if (f.getAnnotation(Select.class) != null) {
                            if (!lists.containsKey(f.getName())) {
                                f.setAccessible(true);
                                final Converter converter = f.getAnnotation(Converter.class);
                                if (converter == null) {
                                    f.set(val, result.getObject(f.getName()));
                                } else {
                                    f.set(
                                            val,
                                            converter.value().getConstructor().newInstance().convert(
                                                    result.getObject(f.getName())
                                            )
                                    );
                                }
                            }
                        }
                    }
                    objs.add(val);
                }
            } catch (IllegalArgumentException | IllegalAccessException | CloneNotSupportedException ex) {
                lib.log(ex);
            }
            for (final String column : lists.keySet()) {
                final DoubleData<Field, QueryObject> ddata = lists.get(column);
                ddata.getLeft().setAccessible(true);

                for (Object val : objs) {
                    ddata.getLeft().set(
                            val,
                            select(
                                    ddata.getRight(),
                                    sifValues,
                                    (QueryObject) val
                            )
                    );
                }
            }
        }
        return (List<T>) objs;
    }
}
