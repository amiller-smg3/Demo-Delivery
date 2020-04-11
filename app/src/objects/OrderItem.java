package objects;

import java.io.Serializable;

public class OrderItem implements Serializable {

    public String OrderItemNumber;
    public String OrderId;
    public String Product2Id;

    public Double UnitPrice;
    public Double Quantity;
    public Double TotalPrice;

    public String ProductName;
    public String Description;

    public OrderItem(){}
}
