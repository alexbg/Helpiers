package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by javi on 28/07/14.
 */
@Entity
public class Category extends Model {

    @Id
    @GeneratedValue
    private Long id;
    private String categoryName;
    @Temporal(TemporalType.DATE)
    private Date creationDate;

    public static Finder<Long,Category> find = new Finder<Long,Category>(Long.class, Category.class);

    public Category(String name){
        categoryName = name;
        creationDate = new Date();
        System.out.println("se ha creado la categoria "+ categoryName + " con fecha " + creationDate);
    }

    //************************************** SINGLETON AUN NO FUNCIONAL
    private static Category INSTANCE = null;

    // Private constructor suppresses
    private Category(){}

    private static void createInstance() {
        if (INSTANCE == null) {
            // Sólo se accede a la zona sincronizada
            // cuando la instancia no está creada
            synchronized(Category.class) {
                // En la zona sincronizada sería necesario volver
                // a comprobar que no se ha creado la instancia
                if (INSTANCE == null) {
                    INSTANCE = new Category();
                }
            }
        }
    }

    public static Category getInstance() {
        if (INSTANCE == null) createInstance();
        return INSTANCE;
    }
    //*********************************************************************

    //********************************************************* GETTERS Y SETTERS
    public String getCategoryName() {
        return categoryName;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
