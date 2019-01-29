/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.gr.java_conf.zakuramomiji.sqllib;

import java.sql.Connection;

/**
 *
 * @author firiz
 */
abstract class QueryRunner {

    final SQLLib lib;
    final Connection conn;
    final boolean debug;

    public QueryRunner(SQLLib lib, Connection conn, boolean debug) {
        this.lib = lib;
        this.conn = conn;
        this.debug = debug;
    }
}
