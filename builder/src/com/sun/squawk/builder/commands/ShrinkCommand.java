package com.sun.squawk.builder.commands;

import com.sun.squawk.builder.*;
import java.io.*;

public class ShrinkCommand extends Command {

	public ShrinkCommand(Build env) {
		super(env, "shrink");
	}
	
	public void run(String[] args) {
		File baseDir = new File(".");
		File cldc = new File(baseDir, "cldc");

		new File("/tmp/d").mkdir();
		{
			File files[] = new File("/tmp/d").listFiles();
			for (int i = 0; i < files.length; i++){
				files[i].delete();
			}
		}
		{
			File files[] = new File(cldc, "j2meclasses").listFiles();
			for (int i = 0; i < files.length; i++){
				files[i].delete();
			}
		}
		env.java("tools/asm-5.1.jar:build.jar", true, "", "com.sun.squawk.builder.asm.Shrink",
				 new String[]{
					 "cldc/classes.target.jar", "/tmp/d", args[0], "cldc/classes.target.jar", args[1]});
		env.preverify("j2meclasses", new File("/tmp/d"), new File(cldc, "j2meclasses"));
	}
}
