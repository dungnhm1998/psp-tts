/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.obj;

/**
 *
 * @author HIEPHV
 */
public class DropshipOrderResultImport {

    private String name;
    private String type;
    private String msg;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DropshipOrderResultImport(String name, String type, String msg) {
        this.name = name;
        this.type = type;
        this.msg = msg;
    }
}
