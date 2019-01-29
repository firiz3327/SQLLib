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
final class Deleter extends QueryRunner {
    
    public Deleter(SQLLib lib, Connection conn, boolean debug) {
        super(lib, conn, debug);
    }
    
}
