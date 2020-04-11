package objects;

import java.io.Serializable;
import java.util.ArrayList;

public class Order implements Serializable {

    public String Id;
    public String orderNumber;
    public ArrayList<OrderItem> listOfItems;

    public Order() {}

    public Order(String sfId, String sfOrderNumber)
    {
        this.Id = sfId;
        this.orderNumber = sfOrderNumber;

        if(listOfItems == null)
        {
            listOfItems = new ArrayList<>();
        }
    }
}
