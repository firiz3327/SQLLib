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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jp.gr.java_conf.zakuramomiji.sqllib.annotation.select.sif.SifValue;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author firiz
 */
public enum SQLLib implements AutoCloseable {
    INSTANCE;

    private final String defaultPropertieParameters[] = {
        "url=jdbc:mysql://localhost:3306/example",
        "user=root",
        "password="
    };
    private final Logger logger = Logger.getLogger(SQLLib.class.getName());
    private Connection conn;

    // runners
    private Selecter selecter;
    private Inserter inserter;
//    private Updater updater;
//    private Deleter deleter;

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
     * @return SQLLib : this
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @since 2018-12-10 / firiz
     *
     */
    public SQLLib setup() throws IOException, SQLException {
        return setup(false);
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
     * @return SQLLib : this
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @since 2018-12-10 / firiz
     *
     */
    public SQLLib setup(final boolean debug) throws IOException, SQLException {
        final File file = new File("db.properties");
        String url;
        String user;
        String password;

        //<editor-fold defaultstate="collapsed" desc="db.propertiesファイルの有無を確認後、読み込みもしくは作成">
        if (file.exists()) {
            try (final InputStream inputstream = new FileInputStream(file)) {
                final Properties prop = new Properties();
                prop.load(inputstream);
                url = prop.getProperty("url");
                user = prop.getProperty("user");
                password = prop.getProperty("password");
            } catch (FileNotFoundException ex) {
                log(ex);
                url = defaultPropertieParameters[0];
                user = defaultPropertieParameters[1];
                password = defaultPropertieParameters[2];
            }
        } else {
            if (file.createNewFile()) {
                try (final PrintWriter writer = new PrintWriter(file)) {
                    for (final String dpp : defaultPropertieParameters) {
                        writer.println(dpp);
                    }
                }
                info("Created a default db.properties file.");
            }
            url = defaultPropertieParameters[0];
            user = defaultPropertieParameters[1];
            password = defaultPropertieParameters[2];
        }
        //</editor-fold>

        // SQLへの接続処理
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
        conn = DriverManager.getConnection(url, user, password);
        conn.setAutoCommit(false);

        selecter = new Selecter(this, conn, debug);
        inserter = new Inserter(this, conn, debug);

        return this;
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }

    public void commit() throws SQLException {
        conn.commit();
    }
    
    protected void info(final Object obj) {
        logger.info(obj.toString());
    }

    protected void info(final String str) {
        logger.info(str);
    }

    protected void log(final Throwable thrown) {
        log(Level.SEVERE, thrown);
    }

    protected void log(final Level level, final Throwable thrown) {
        log(level, null, thrown);
    }

    protected void log(final Level level, final String msg, final Throwable thrown) {
        logger.log(level, msg, thrown);
    }

    public <T extends QueryObject> List<T> select(@NotNull final T obj) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        return selecter.select(obj);
    }
    public <T extends QueryObject> List<T> select(@NotNull final T obj, SifValue... sifValues) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        return selecter.select(obj, sifValues);
    }

    public void insert(@NotNull final List<QueryObject> objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        inserter.insert(true, objs.toArray(new QueryObject[objs.size()]));
    }

    public void insert(final boolean commit, @NotNull final List<QueryObject> objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        inserter.insert(commit, objs.toArray(new QueryObject[objs.size()]));
    }

    public void insert(@NotNull final QueryObject... objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        inserter.insert(true, objs);
    }

    public void insert(final boolean commit, @NotNull final QueryObject... objs) throws SQLException, IllegalArgumentException, IllegalAccessException {
        inserter.insert(commit, objs);
    }

}
