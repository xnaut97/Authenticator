package com.github.tezvn.authenticator.utils.time;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

public class TimeUtils {
	
	private long oldTime;
	
	private long newTime;
	
	private final String numFormat = "[0-9]+[\\.]?[0-9]*";
	
	private TimeUtils(long oldTime) {
		if(oldTime < 1)
			throw new IllegalArgumentException("oldTime must equals or greater than 1");
		this.oldTime = oldTime;
		this.newTime = oldTime;
	}
	
	private TimeUtils(long oldTime, long newTime) {
		this.oldTime = oldTime;
		this.newTime = newTime;
	}
	
	public static TimeUtils of(long oldTime) {
		return new TimeUtils(oldTime);
	}
	
	public static TimeUtils of(long oldTime, long newTime) {
		return new TimeUtils(oldTime, newTime);
	}

	public static TimeUtils newInstance() {
		return new TimeUtils(System.currentTimeMillis());
	}

	public long getOldTime() {
		return oldTime;
	}
	
	public TimeUtils setOldTime(long oldTime) {
		this.oldTime = oldTime;
		return this;
	}
	
	public long getNewTime() {
		return newTime;
	}

	public long getTicks() {
		Date oldDate = new Date(this.oldTime);
		Date newDate = new Date(this.newTime);
		LocalDateTime old = LocalDateTime.ofInstant(oldDate.toInstant(), ZoneId.systemDefault());
		LocalDateTime current = LocalDateTime.ofInstant(newDate.toInstant(), ZoneId.systemDefault());
		int seconds = (int)Duration.between(old, current).getSeconds();
		int secs = (((seconds % 604800) % 86400) % 3600) % 60;
		return secs*20;
	}
	
	public TimeUtils setNewTime(long newTime) {
		this.newTime = newTime;
		return this;
	}
	
	public String format() {
		if(this.newTime < 1)
			return null;
		return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(this.newTime);
	}
	
	public String format(String pattern) {
		if(this.newTime < 1)
			return null;
		return new SimpleDateFormat(pattern).format(this.newTime);
	}

	public TimeUtils add(long time) {
		this.newTime += time;
		return this;
	}

	public TimeUtils add(TimeUnits unit, int amount) {
		return modifyTime(unit, amount, 0);
	}

	public TimeUtils addTicks(long ticks) {
		int toAdd = (int) (ticks/20)*60;
		if(toAdd <= 0)
			return this;
		this.add(TimeUnits.SECOND, toAdd);
		return this;
	}

	public TimeUtils subtract(TimeUnits unit, int amount) {
		return modifyTime(unit, amount, 1);
	}
	
	private TimeUtils modifyTime(TimeUnits unit, int amount, int operation) {
		Date date = new Date(newTime);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		switch(unit) {
		case DAY:
			calendar.add(5, operation > 0 ? -amount : amount);
			break;
		case HOUR:
			calendar.add(11, operation > 0 ? -amount : amount);
			break;
		case MINUTE:
			calendar.add(12, operation > 0 ? -amount : amount);
			break;
		case MONTH:
			calendar.add(2, operation > 0 ? -amount : amount);
			break;
		case SECOND:
			calendar.add(13, operation > 0 ? -amount : amount);
			break;
		case WEEK:
			calendar.add(3, operation > 0 ? -amount : amount);
			break;
		case YEAR:
			calendar.add(1, operation > 0 ? -amount : amount);
			break;
		}
		this.newTime = calendar.getTime().getTime();
		return this;
	}
	
	public TimeUtils add(String format) {
		return modifyDuration(format, 0);
	}
	
	public TimeUtils subtract(String format) {
		return modifyDuration(format, 1);
	}
	
	private TimeUtils modifyDuration(String format, int math) {
		StringBuilder numbers = new StringBuilder();
		StringBuilder units = new StringBuilder();
		for(int i = 0; i < format.length(); i++) {
			String str = String.valueOf(format.charAt(i));
			if(isNumber(str)) {
				numbers.append(str);
			}else {
				units.append(str).append(",");
				numbers.append(",");
			}
		}
		if(units.length() < 1)
			return this;
		String[] splitNumbers = numbers.toString().split(",");
		String[] splitUnits = units.toString().split(",");
		for(int i = 0; i < splitUnits.length; i++) {
			Optional<TimeUnits> opt = TimeUnits.parse(splitUnits[i]);
			if(!opt.isPresent())
				continue;
			TimeUnits unit = opt.get();
			int amount = Math.abs(Integer.parseInt(splitNumbers[i]));
			
			if(math == 0)
				this.add(unit, amount);
			else if(math == 1)
				this.subtract(unit, amount);
		}
		return this;
	}
	
	public String getFullDuration() {
		Date oldDate = new Date(this.oldTime);
		Date newDate = new Date(this.newTime);
		LocalDateTime old = LocalDateTime.ofInstant(oldDate.toInstant(), ZoneId.systemDefault());
		LocalDateTime current = LocalDateTime.ofInstant(newDate.toInstant(), ZoneId.systemDefault());
		int seconds = (int)Duration.between(old, current).getSeconds();
		int years = seconds / 31104000;
		int months = seconds / (2419200+172800);
		int weeks = seconds / 604800;
		int days = (seconds % 604800) / 86400;
		int hours = ((seconds % 604800) % 86400) / 3600;
		int mins = (((seconds % 604800) % 86400) % 3600) / 60;
		int secs = (((seconds % 604800) % 86400) % 3600) % 60;
		String yearsFormat = years > 0 ? years + "y " : "";
		String monthsFormat = months > 0 ? months + "M " : "";
		String weeksFormat = weeks > 0 ? weeks + "w " : "";
		String daysFormat = (days > 0 ? days + "d " : "");
		String hoursFormat = (hours > 0 ? hours + "h " : "");
		String minsFormat = (mins > 0 ? mins + "m " : "");
		String secsFormat = (secs > 0 ? secs + "s " : "0s");
		return yearsFormat + monthsFormat + weeksFormat + daysFormat + hoursFormat + minsFormat + secsFormat;
	}
	
	public String getShortDuration() {
		Date oldDate = new Date(this.oldTime);
		Date newDate = new Date(this.newTime);
		LocalDateTime old = LocalDateTime.ofInstant(oldDate.toInstant(), ZoneId.systemDefault());
		LocalDateTime current = LocalDateTime.ofInstant(newDate.toInstant(), ZoneId.systemDefault());
		int seconds = (int)Duration.between(old, current).getSeconds();
		int years = seconds / 31104000 % 365;
		int months = seconds / (2419200+172800) % 12;
		int weeks = seconds / 604800;
		int days = (seconds % 604800) / 86400;
		int hours = ((seconds % 604800) % 86400) / 3600;
		int mins = (((seconds % 604800) % 86400) % 3600) / 60;
		int secs = (((seconds % 604800) % 86400) % 3600) % 60;
		String yearsFormat = years > 0 ? years + "y " : "";
		String monthsFormat = months > 0 ? months + "M " : "";
		String weeksFormat = weeks > 0 ? weeks + "w " : "";
		String daysFormat = (days > 0 ? days + "d " : "");
		String hoursFormat = (hours > 0 ? hours + "h " : "");
		String minsFormat = (mins > 0 ? mins + "m " : "");
		String secsFormat = (secs > 0 ? secs + "s " : "0s");
		
		if(years > 0)
			return yearsFormat + monthsFormat;
		else if(months > 0)
			return monthsFormat + weeksFormat;
		else if(weeks > 0)
			return weeksFormat + daysFormat;
		else if(days > 0)
			return daysFormat + hoursFormat;
		else if(hours > 0)
			return hoursFormat + minsFormat;
		else if(mins > 0)
			return minsFormat + secsFormat;
		else if(secs > 0)
			return secsFormat;
		return "";
	}
	
	public static Date toDate(String format) {
		if(format == null || format.isEmpty())
			throw new IllegalArgumentException("format cannot be null!");
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		try {
			return sdf.parse(format);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static long toTimeMillis(String format) {
		return toDate(format).getTime();
	}
	
	private boolean isNumber(String str) {
		return Pattern.matches(this.numFormat, str);
	}
	
}
