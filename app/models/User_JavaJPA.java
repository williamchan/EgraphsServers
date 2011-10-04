package models;

import play.db.jpa.JPABase;
import play.db.jpa.Model;

import javax.persistence.Entity;
import java.util.List;

@Entity
public class User_JavaJPA extends Model {

    public String name;
    public String email;
    public String address;

    public boolean create_Scala() {
        return super.create();
    }

    public <T extends JPABase> T save_Scala() {
        return super.save();
    }

    public boolean validateAndCreate_Scala() {
        return super.validateAndCreate();
    }

    public void delete_Scala() {
        super.delete();
    }

    public <T extends JPABase> T refresh_Scala() {
        return super.refresh();
    }

    public List<JPABase> me() {
//        User_JavaJPA.find()
        return User_JavaJPA.findAll();
    }


}