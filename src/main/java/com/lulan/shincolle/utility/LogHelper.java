package com.lulan.shincolle.utility;

import org.apache.logging.log4j.Level;

import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.FMLLog;

public class LogHelper {
	
	//
	public static void log(Level logLevel, Object object) {
		FMLLog.log(Reference.MOD_NAME,logLevel,String.valueOf(object));
	}
	
	//off: ������log
	public static void off(Object object) { log(Level.OFF,object); }
	
	//fatal: �����{������B�@���P�R���D
	public static void fatal(Object object) { log(Level.FATAL,object); }
	
	//error: �����{���i�ఱ��B�@�����D
	public static void error(Object object) { log(Level.ERROR,object); }
	
	//warn: �����i��ɭPerror�����D
	public static void warn(Object object) { log(Level.WARN,object); }
	
	//info: �������ܰT��
	public static void info(Object object) { log(Level.INFO,object); }
	
	//debug: ����debug�T��
	public static void debug(Object object) { log(Level.DEBUG,object); }
	
	//trace: �Pdebug, �`�Ω�l�ܵ{���ʦV
	public static void trace(Object object) { log(Level.TRACE,object); }
	
	//all: ���������T��
	public static void all(Object object) { log(Level.ALL,object); }
}
