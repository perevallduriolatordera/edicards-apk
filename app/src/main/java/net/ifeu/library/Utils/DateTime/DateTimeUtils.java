package net.ifeu.library.Utils.DateTime;

import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {

    public static Date getFirstDayOfCurrentWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysToSubstract = 0;

        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                daysToSubstract = -6;
                break;
            case Calendar.MONDAY:
                daysToSubstract = 0;
                break;
            case Calendar.TUESDAY:
                daysToSubstract = -1;
                break;
            case Calendar.WEDNESDAY:
                daysToSubstract = -2;
                break;
            case Calendar.THURSDAY:
                daysToSubstract = -3;
                break;
            case Calendar.FRIDAY:
                daysToSubstract = -4;
                break;
            case Calendar.SATURDAY:
                daysToSubstract = -5;
                break;
        }

        calendar.add( Calendar.DAY_OF_YEAR, daysToSubstract);
        return calendar.getTime();

    }
}
