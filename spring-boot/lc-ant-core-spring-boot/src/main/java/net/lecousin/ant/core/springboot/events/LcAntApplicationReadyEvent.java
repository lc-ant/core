package net.lecousin.ant.core.springboot.events;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

public class LcAntApplicationReadyEvent extends ApplicationContextEvent {

	private static final long serialVersionUID = 1L;

	public LcAntApplicationReadyEvent(ApplicationContext source) {
		super(source);
	}

}
