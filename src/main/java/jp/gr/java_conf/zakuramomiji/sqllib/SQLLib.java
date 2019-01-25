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
    public <T extends QueryObject> List<T> select(@NotNull final QueryObject obj) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        return select(obj, null);
    }

    private <T extends QueryObject> List<T> select(@NotNull final QueryObject obj, final QueryObject parent) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
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

        System.out.println(query.toString());

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
    public PreparedStatement getInsertPreparedStatement(@NotNull final QueryObject obj) throws SQLException {
        final StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(obj.getTable()).append("(");

        final List<Field> updateFields = new ArrayList<>();
        final Field[] fields = obj.getClass().getDeclaredFields();
        int j = 0;
        for (int i = 0; i < fields.length; i++) {
            final Field f = fields[i];
            final Insert insert = f.getAnnotation(Insert.class);
            if (insert != null) {
                if (insert.update()) {
                    updateFields.add(f);
                }
                query.append(i == 0 ? "" : ",").append(f.getName());
                j++;
            }
        }
        query.append(") VALUES (");
        for (int i = 0; i < j; i++) {
            query.append(i == 0 ? "?" : ",?");
        }
        query.append(")");
        if (!updateFields.isEmpty()) {
            query.append(" ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < updateFields.size(); i++) {
                final Field f = updateFields.get(i);
                query.append(i == 0 ? "" : ",").append(f.getName()).append("=?");
            }
        }
        return conn.prepareStatement(query.toString());
    }

    @NotNull
    public DoubleData<PreparedStatement, List<Object>> getInsertPreparedStatementAndValues(@NotNull final QueryObject obj) throws SQLException, IllegalAccessException {
        final Map<String, DoubleData<Field, QueryObject>> lists = new LinkedHashMap<>();
        final StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(obj.getTable()).append("(");

        final List<Object> values = new ArrayList<>();
        final List<Field> updateFields = new ArrayList<>();
        final Field[] fields = obj.getClass().getDeclaredFields();
        int j = 0;
        for (int i = 0; i < fields.length; i++) {
            final Field f = fields[i];
            if (f.getType() == List.class) {
                f.setAccessible(true);
                final String name = f.getName();
                try {
                    final Class type = (Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                    final Constructor c = type.isMemberClass()
                            ? type.getDeclaredConstructor(obj.getClass())
                            : type.getDeclaredConstructor();
                    lists.put(name, new DoubleData<>(f, (QueryObject) (type.isMemberClass() ? c.newInstance(obj) : c.newInstance())));
                } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalArgumentException | InvocationTargetException ex) {
                    log(Level.SEVERE, null, ex);
                }
            } else {
                final Insert insert = f.getAnnotation(Insert.class);
                if (insert != null) {
                    if (insert.update()) {
                        updateFields.add(f);
                    }
                    query.append(i == 0 ? "" : ",").append(f.getName());

                    f.setAccessible(true);
                    values.add(f.get(obj));
                    j++;
                }
            }
        }
        query.append(") VALUES (");
        for (int i = 0; i < j; i++) {
            query.append(i == 0 ? "?" : ",?");
        }
        query.append(")");
        if (!updateFields.isEmpty()) {
            query.append(" ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < updateFields.size(); i++) {
                final Field f = updateFields.get(i);
                query.append(i == 0 ? "" : ",").append(f.getName()).append("=?");

                f.setAccessible(true);
                values.add(f.get(obj));
            }
        }
        for (final String column : lists.keySet()) {
            final DoubleData<Field, QueryObject> ddata = lists.get(column);
            ddata.getLeft().setAccessible(true);
            ///////
        }
        return new DoubleData<>(conn.prepareStatement(query.toString()), values);
    }

    @NotNull
    public void insert(@NotNull final QueryObject obj) throws SQLException, IllegalAccessException {
        final DoubleData<PreparedStatement, List<Object>> ips = getInsertPreparedStatementAndValues(obj);
        final PreparedStatement ps = ips.getLeft();
        try (ps) {
            insert(obj, ps, ips.getRight());
            conn.commit();
        }
    }

    @NotNull
    public void insert(@NotNull final QueryObject obj, @NotNull final PreparedStatement ps, @NotNull final List<Object> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            ps.setObject(i + 1, values.get(i));
        }
        ps.executeUpdate();
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
