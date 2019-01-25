/*
 * SQLLib.java
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

import jp.gr.java_conf.zakuramomiji.sqllib.utils.QueryObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.Same;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.Select;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.sif.Sif;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.Table;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.insert.Insert;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.TSame;
import jp.gr.java_conf.zakuramomiji.sqllib.utils.DoubleData;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author firiz
 */
public enum SQLLib {
    INSTANCE;

    private final String defaultPropertieParameters[] = {
        "url=jdbc:mysql://localhost:3306/example",
        "user=root",
        "password="
    };
    private final Logger logger = Logger.getLogger(SQLLib.class.getName());
    private boolean debug = false;
    private String url;
    private String user;
    private String password;
    private Connection conn;

    /**
     * Setup SQLLib
     *
     * <p>
     * カレントディレクトリにdb.propertiesファイルが存在する場合は読み込み、
     * 存在しない場合は、下記の設定のプロパティファイルを作成しSQLに接続します。
     * </p>
     *
     * <ul>
     * <li>{@code url=jdbc:mysql://localhost:3306/example}</li>
     * <li>{@code user=root}</li>
     * <li>{@code password=}</li>
     * </ul>
     *
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @since 2018-12-10 / firiz
     *
     */
    public void setup() throws IOException, SQLException {
        setup(false);
    }

    /**
     * Setup SQLLib
     *
     * <p>
     * カレントディレクトリにdb.propertiesファイルが存在する場合は読み込み、
     * 存在しない場合は、下記の設定のプロパティファイルを作成しSQLに接続します。
     * </p>
     *
     * <ul>
     * <li>{@code url=jdbc:mysql://localhost:3306/example}</li>
     * <li>{@code user=root}</li>
     * <li>{@code password=}</li>
     * </ul>
     *
     * @param debug boolean : デバッグモード
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @since 2018-12-10 / firiz
     *
     */
    public void setup(final boolean debug) throws IOException, SQLException {
        this.debug = debug;
        final File file = new File("db.properties");

        //<editor-fold defaultstate="collapsed" desc="db.propertiesファイルの有無を確認後、読み込みもしくは作成">
        check_properties:
        try (final InputStream inputstream = new FileInputStream(file)) {
            final Properties prop = new Properties();
            prop.load(inputstream);
            url = prop.getProperty("url");
            user = prop.getProperty("user");
            password = prop.getProperty("password");
        } catch (FileNotFoundException ex) {
            if (file.createNewFile()) {
                try (final PrintWriter writer = new PrintWriter(file)) {
                    for (final String dpp : defaultPropertieParameters) {
                        writer.println(dpp);
                    }
                }
                url = defaultPropertieParameters[0];
                user = defaultPropertieParameters[1];
                password = defaultPropertieParameters[2];
                info("Created a default db.properties file.");
                break check_properties;
            }
            logger.log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        // SQLへの接続処理
        conn = DriverManager.getConnection(url, user, password);
        conn.setAutoCommit(false);
    }

    public void commit() throws SQLException {
        conn.commit();
    }

    //<editor-fold defaultstate="collapsed" desc="select methods">
    public <T extends QueryObject> List<T> select(@NotNull final T obj) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        return select(obj, null);
    }

    private <T extends QueryObject> List<T> select(@NotNull final T obj, final QueryObject parent) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        final Map<String, Select> selects = new LinkedHashMap<>();
        final Map<String, Table> tables = new LinkedHashMap<>();
        final Map<String, Sif> sifs = new LinkedHashMap<>();
        final Map<String, Same[]> sames = new LinkedHashMap<>();
        final Map<String, DoubleData<Field, QueryObject>> lists = new LinkedHashMap<>();

        final Field[] fields = obj.getClass().getDeclaredFields();
        for (final Field f : fields) {
            f.setAccessible(true);
            final String name = f.getName();

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
        if (!sifs.isEmpty() || !sames.isEmpty() || tsame != null) {
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
            System.out.println(query.toString());
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
                                f.set(val, result.getObject(f.getName()));
                            }
                        }
                    }
                    objs.add(val);
                }
            } catch (IllegalArgumentException | IllegalAccessException | CloneNotSupportedException ex) {
                log(Level.SEVERE, null, ex);
            }
            for (final String column : lists.keySet()) {
                final DoubleData<Field, QueryObject> ddata = lists.get(column);
                ddata.getLeft().setAccessible(true);

                for (Object val : objs) {
                    ddata.getLeft().set(val, select(ddata.getRight(), (QueryObject) val));
                }
            }
        }
        return (List<T>) objs;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="insert methods">
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
                        final Constructor c = type.isMemberClass()
                                ? type.getDeclaredConstructor(objs[0].getClass())
                                : type.getDeclaredConstructor();
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
                    } catch (NoSuchMethodException | SecurityException | IllegalArgumentException ex) {
                        log(Level.SEVERE, null, ex);
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
                        v.add(f.get(obj));
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
                    v.add(f.get(obj));
                }
            }
            values.add(v);
        }
        System.out.println("values - " + values);
        if (debug) {
            System.out.println(query.toString());
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
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="log methods">
    public void info(final String str) {
        logger.info(str);
    }

    public void log(final Throwable thrown) {
        log(Level.SEVERE, thrown);
    }

    public void log(final Level level, final Throwable thrown) {
        log(level, null, thrown);
    }

    public void log(final Level level, final String msg, final Throwable thrown) {
        logger.log(level, msg, thrown);
    }
    //</editor-fold>
}
