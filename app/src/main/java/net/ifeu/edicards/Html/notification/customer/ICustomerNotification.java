package net.ifeu.edicards.Html.notification.customer;

public interface ICustomerNotification<T> {
    void notify(T t);
}
