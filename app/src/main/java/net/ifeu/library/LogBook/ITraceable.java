package net.ifeu.library.LogBook;

import java.util.ArrayList;
import java.util.Date;

public interface ITraceable<T> {

    void save() throws Exception;
    boolean hasLogBookCurrentWeek() throws Exception;
    ArrayList<T> getLogBookLastPeriod(Date today) throws Exception;
    void purge(Date today) throws Exception;
}