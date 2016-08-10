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

		new File(cldc, "classes.target").renameTo(new File(cldc, "classes.target.orig"));
		new File(cldc, "j2meclasses").renameTo(new File(cldc, "j2meclasses.orig"));
		
		env.mkdir(cldc, "classes.target");
		env.mkdir(cldc, "j2meclasses");

		env.java("tools/asm-5.1.jar:build.jar", false, "", "com.sun.squawk.builder.asm.Shrink",
				 new String[]{
					 "cldc/classes.target.orig", "cldc/classes.target", args[0], "cldc/classes.target.orig", args[1]});
		env.preverify("j2meclasses", cldc);
	}
}
