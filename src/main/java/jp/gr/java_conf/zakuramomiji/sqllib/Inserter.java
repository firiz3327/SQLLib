/*
 * Inserter.java
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.convert.Converter;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.insert.Insert;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.TSame;
import jp.gr.java_conf.zakuramomiji.sqllib.utils.DoubleData;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author firiz
 */
final class Inserter extends QueryRunner {

    public Inserter(SQLLib lib, Connection conn, boolean debug) {
        super(lib, conn, debug);
    }

    public void insert(@NotNull final List<QueryObject> objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        insert(true, null, objs.toArray(new QueryObject[objs.size()]));
    }

    public void insert(final boolean commit, @NotNull final List<QueryObject> objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        insert(commit, null, objs.toArray(new QueryObject[objs.size()]));
    }

    private void insert(final boolean commit, final DoubleData<TSame, List<Object>> tsame_datas, @NotNull final List<QueryObject> objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        insert(commit, tsame_datas, objs.toArray(new QueryObject[objs.size()]));
    }

    public void insert(@NotNull final QueryObject... objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        insert(true, null, objs);
    }

    public void insert(final boolean commit, @NotNull final QueryObject... objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        insert(commit, null, objs);
    }

    private void insert(final boolean commit, final DoubleData<TSame, List<Object>> tsame_datas, @NotNull final QueryObject... objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        final Map<String, List<DoubleData<QueryObject, DoubleData<TSame, Object>>>> lists = new LinkedHashMap<>();
        final StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(objs[0].getTable()).append("(");

        final List<List<Object>> values = new ArrayList<>();

        boolean firstInto = true;
        if (tsame_datas != null) {
            query.append(firstInto ? "" : ",").append(tsame_datas.getLeft().column());
            firstInto = false;
        }
        for (int l = 0; l < objs.length; l++) {
            final List<Field> updateFields = new ArrayList<>();
            final List<Object> v = new ArrayList<>();
            if (tsame_datas != null) {
                v.add(tsame_datas.getRight().get(l));
            }

            final QueryObject obj = objs[l];
            final Field[] fields = obj.getClass().getDeclaredFields();
            for (final Field f : fields) {
                if (f.getType() == List.class) {
                    f.setAccessible(true);
                    final String name = f.getName();
                    try {
                        final Class type = (Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                        final TSame ts = (TSame) type.getAnnotation(TSame.class);
                        final List<DoubleData<QueryObject, DoubleData<TSame, Object>>> dds = new ArrayList<>();
                        for (final QueryObject qo : (List<QueryObject>) f.get(obj)) {
                            dds.add(new DoubleData<>(
                                    qo,
                                    ts == null ? null : new DoubleData<>(ts, obj.get(ts.origin()))
                            ));
                        }
                        if (lists.containsKey(name)) {
                            lists.get(name).addAll(dds);
                        } else {
                            lists.put(name, dds);
                        }
                    } catch (SecurityException | IllegalArgumentException ex) {
                        lib.log(ex);
                    }
                } else {
                    final Insert insert = f.getAnnotation(Insert.class);
                    if (insert != null) {
                        if (insert.update()) {
                            updateFields.add(f);
                        }
                        if (l == 0) {
                            query.append(firstInto ? "" : ",").append(f.getName());
                            firstInto = false;
                        }

                        f.setAccessible(true);
                        final Converter converter = f.getAnnotation(Converter.class);
                        if (converter == null) {
                            v.add(f.get(obj));
                        } else {
                            try {
                                v.add(
                                        converter.value()
                                                .getConstructor()
                                                .newInstance()
                                                .reverse(f.get(obj))
                                );
                            } catch (NoSuchMethodException | SecurityException | InstantiationException | InvocationTargetException ex) {
                                lib.log(ex);
                            }
                        }
                    }
                }
            }
            if (l == 0) {
                query.append(") VALUES (");
                for (int i = 0; i < v.size(); i++) {
                    query.append(i == 0 ? "?" : ",?");
                }
                query.append(")");
            }
            if (!updateFields.isEmpty()) {
                if (l == 0) {
                    query.append(" ON DUPLICATE KEY UPDATE ");
                }
                for (int i = 0; i < updateFields.size(); i++) {
                    final Field f = updateFields.get(i);
                    if (l == 0) {
                        query.append(i == 0 ? "" : ",").append(f.getName()).append("=?");
                    }

                    f.setAccessible(true);
                    final Converter converter = f.getAnnotation(Converter.class);
                    if (converter == null) {
                        v.add(f.get(obj));
                    } else {
                        try {
                            v.add(
                                    converter.value()
                                            .getConstructor()
                                            .newInstance()
                                            .reverse(f.get(obj))
                            );
                        } catch (NoSuchMethodException | SecurityException | InstantiationException | InvocationTargetException ex) {
                            lib.log(ex);
                        }
                    }
                }
            }
            values.add(v);
        }
        if (debug) {
            lib.info("values - " + values);
            lib.info(query);
        }
        try (final PreparedStatement ps = conn.prepareStatement(query.toString())) {
            for (int i = 0; i < objs.length; i++) {
                final QueryObject obj = objs[i];
                if (objs[0].getClass() == obj.getClass()) {
                    ps.clearParameters();
                    final List<Object> vs = values.get(i);
                    for (int j = 0; j < vs.size(); j++) {
                        ps.setObject(j + 1, vs.get(j));
                    }
                    ps.addBatch();
                } else {
                    throw new IllegalArgumentException("All arguments should be of the same class.");
                }
            }
            ps.executeBatch();
        }
        for (final String column : lists.keySet()) {
            final List<QueryObject> qos = new ArrayList<>();
            final DoubleData<TSame, List<Object>> next_tsame_datas = new DoubleData<>(null, new ArrayList<>());
            for (final DoubleData<QueryObject, DoubleData<TSame, Object>> dd : lists.get(column)) {
                qos.add(dd.getLeft());
                if (next_tsame_datas.getLeft() == null) {
                    next_tsame_datas.setLeft(dd.getRight().getLeft());
                }
                next_tsame_datas.getRight().add(dd.getRight().getRight());
            }
            insert(
                    false,
                    next_tsame_datas,
                    qos
            );
        }
        if (commit) {
            conn.commit();
        }
    }
}
