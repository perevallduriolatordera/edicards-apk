package net.ifeu.edicards.Html.notification.customer;

import net.ifeu.edicards.Application.AppConfig;

public interface ICustomerNotification<T> {
    void notify(T t, AppConfig appConfig);
}
