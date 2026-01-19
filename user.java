/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package login;

/**
 *
 * @author attia
 */
public class user {
    String nama, username, password, hakakses;
    
    public String getnama(){
        return nama;
    }
    
    public void setnama(String nama){
        this.nama = nama;
    }
    
    public String getusername(){
        return username;
    }
    
    public void setusername(String username){
        this.username = username;
    }
    
    public String getpassword(){
        return password;
    }
    
    public void setpassword(String password){
        this.password = password;
    }
    
    public String gethakakses(){
        return hakakses;
    }

    public void sethakakses(String hakakses){
        this.hakakses = hakakses;
    }
    
    int id_user;
    public int getiduser(){
        return id_user;
    }
    
    public void setiduser(int id_user){
        this.id_user = id_user;
    }
    
}
