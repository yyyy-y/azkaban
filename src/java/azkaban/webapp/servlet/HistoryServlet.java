package azkaban.webapp.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import azkaban.executor.ExecutorManager;
import azkaban.executor.ExecutorManager.ExecutionReference;
import azkaban.utils.JSONUtils;
import azkaban.webapp.session.Session;

public class HistoryServlet extends LoginAbstractAzkabanServlet {

	private static final long serialVersionUID = 1L;
	private ExecutorManager executorManager;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		executorManager = this.getApplication().getExecutorManager();
	}

	@Override
	protected void handleGet(HttpServletRequest req, HttpServletResponse resp,
			Session session) throws ServletException, IOException {
		
		if (hasParam(req, "timeline")) {
			handleHistoryTimelinePage(req, resp, session);
		}
		else {
			handleHistoryPage(req, resp, session);
		}
	}

	@Override
	protected void handlePost(HttpServletRequest req, HttpServletResponse resp, Session session) throws ServletException, IOException {
		if (hasParam(req, "action")) {
			String action = getParam(req, "action");
			if (action.equals("search")) {
				String searchTerm = getParam(req, "searchterm");
				if(!searchTerm.equals("") && !searchTerm.equals(".*")) {
					Page page = newPage(req, resp, session, "azkaban/webapp/servlet/velocity/historypage.vm");
					int pageNum = getIntParam(req, "page", 1);
					int pageSize = getIntParam(req, "size", 16);
				
					if (pageNum < 0) {
						pageNum = 1;
					}
		
					List<ExecutionReference> history = executorManager.getFlowHistory(".*", searchTerm, ".*", 0, DateTime.now().getMillis(), pageSize, (pageNum - 1)*pageSize, true);
					page.add("flowHistory", history);
					page.add("size", pageSize);
					page.add("page", pageNum);
					page.add("search_term", searchTerm);
		
					if (pageNum == 1) {
						page.add("previous", new PageSelection(1, pageSize, true, false));
					}
					page.add("next", new PageSelection(pageNum + 1, pageSize, false, false));
						// Now for the 5 other values.
					int pageStartValue = 1;
					if (pageNum > 3) {
						pageStartValue = pageNum - 2;
					}
		
					page.add("page1", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
					pageStartValue++;
					page.add("page2", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
					pageStartValue++;
					page.add("page3", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
					pageStartValue++;
					page.add("page4", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
					pageStartValue++;
					page.add("page5", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
					pageStartValue++;
		
					page.render();
				}
				else resp.sendRedirect(req.getRequestURL().toString());
			}
			else resp.sendRedirect(req.getRequestURL().toString());
		}
		else {
			resp.sendRedirect(req.getRequestURL().toString());
		}
		
	}

	private void handleHistoryPage(HttpServletRequest req, HttpServletResponse resp, Session session) throws ServletException {
		Page page = newPage(req, resp, session, "azkaban/webapp/servlet/velocity/historypage.vm");
		int pageNum = getIntParam(req, "page", 1);
		int pageSize = getIntParam(req, "size", 16);
		
		if (pageNum < 0) {
			pageNum = 1;
		}
		List<ExecutionReference> history = null;
		if(hasParam(req, "advfilter")) {
			String projRe = getParam(req, "projre").equals("") ? ".*" : getParam(req, "projre");
			String flowRe = getParam(req, "flowre").equals("") ? ".*" : getParam(req, "flowre");
			String userRe = getParam(req, "userre").equals("") ? ".*" : getParam(req, "userre");
			long beginTime = getParam(req, "begin").equals("") ? 0 : DateTimeFormat.forPattern("MM/dd/yyyy").parseDateTime(getParam(req, "begin")).getMillis();
			long endTime = getParam(req, "end").equals("") ? DateTime.now().getMillis() : DateTimeFormat.forPattern("MM/dd/yyyy").parseDateTime(getParam(req, "end")).getMillis();
			history = executorManager.getFlowHistory(projRe, flowRe, userRe, beginTime, endTime, pageSize, (pageNum - 1)*pageSize, true);
		}
		else {
			history = executorManager.getFlowHistory("", "", "", 0, 0, pageSize, (pageNum - 1)*pageSize, false);
		}
		page.add("flowHistory", history);
		page.add("size", pageSize);
		page.add("page", pageNum);
		
		if (pageNum == 1) {
			page.add("previous", new PageSelection(1, pageSize, true, false));
		}
		page.add("next", new PageSelection(pageNum + 1, pageSize, false, false));
		// Now for the 5 other values.
		int pageStartValue = 1;
		if (pageNum > 3) {
			pageStartValue = pageNum - 2;
		}
		
		page.add("page1", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
		pageStartValue++;
		page.add("page2", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
		pageStartValue++;
		page.add("page3", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
		pageStartValue++;
		page.add("page4", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
		pageStartValue++;
		page.add("page5", new PageSelection(pageStartValue, pageSize, false, pageStartValue == pageNum));
		pageStartValue++;
		
		page.render();
	}
	
	private void handleHistoryTimelinePage(HttpServletRequest req, HttpServletResponse resp, Session session) {
		Page page = newPage(req, resp, session, "azkaban/webapp/servlet/velocity/historytimelinepage.vm");
		long currentTime = System.currentTimeMillis();
		long begin = getLongParam(req, "begin", currentTime - 86400000);
		long end = getLongParam(req, "end", currentTime);
		
		page.add("begin", begin);
		page.add("end", end);
		
		List<ExecutionReference> refs = executorManager.getFlowHistory(begin, end);
		ArrayList<Object> refList = new ArrayList<Object>();
		for (ExecutionReference ref: refs) {
			
			HashMap<String,Object> refObj = new HashMap<String,Object>();
			refObj.put("execId", ref.getExecId());
			refObj.put("start", ref.getStartTime());
			refObj.put("end", ref.getEndTime());
			refObj.put("status", ref.getStatus().toString());
			
			refList.add(refObj);
		}
		
		page.add("data", JSONUtils.toJSON(refList));
		page.render();
	}
	
	public class PageSelection {
		private int page;
		private int size;
		private boolean disabled;
		private boolean selected;
		
		public PageSelection(int page, int size, boolean disabled, boolean selected) {
			this.page = page;
			this.size = size;
			this.disabled = disabled;
			this.setSelected(selected);
		}
		
		public int getPage() {
			return page;
		}
		
		public int getSize() {
			return size;
		}
		
		public boolean getDisabled() {
			return disabled;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}
}
