package org.sitenv.spring.service;

import java.util.List;
import org.sitenv.spring.dao.GroupDao;
import org.sitenv.spring.model.DafGroup;
import org.sitenv.spring.model.DafGroup;
import org.sitenv.spring.util.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("GroupService")
@Transactional
public class GroupServiceImpl implements GroupService {
	@Autowired
	private GroupDao groupDao;

	public DafGroup getGroupById(String id) {
		return this.groupDao.getGroupById(id);
	}

	public List<DafGroup> search(SearchParameterMap theMap) {
		return this.groupDao.search(theMap);
	}

	public void createGroups(DafGroup dafGroup) {
		this.groupDao.createGroup(dafGroup);
	}
}
