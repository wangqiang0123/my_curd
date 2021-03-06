package com.hxkj.system.controller;

import com.hxkj.common.constant.Constant;
import com.hxkj.common.util.BaseController;
import com.hxkj.common.util.Identities;
import com.hxkj.common.util.search.SearchSql;
import com.hxkj.system.model.SysRole;
import com.hxkj.system.model.SysUser;
import com.hxkj.system.model.SysUserRole;
import com.jfinal.aop.Before;
import com.jfinal.kit.HashKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.tx.Tx;

import java.util.*;

/**
 * 系统用户 控制器
 */
public class SysUserController extends BaseController {

    public void index() {
        render("system/sysUser.html");
    }


    @Before(SearchSql.class)
    public void query() {
        int pageNumber = getAttr("pageNumber");
        int pageSize = getAttr("pageSize");
        String where = getAttr(Constant.SEARCH_SQL);
        Page<SysUser> sysUsers = SysUser.dao.page(pageNumber, pageSize, where);
        renderDatagrid(sysUsers);
    }

    public void newModel() {
        String id = getPara("id");
        if (id != null) {
            SysUser sysUser = SysUser.dao.findById(id);
            setAttr("sysUser", sysUser);
        }
        render("system/sysUser_form.html");
    }

    public void addAction() {
        SysUser sysUser = getBean(SysUser.class, "");
        sysUser.setId(Identities.uuid2());
        String password = HashKit.sha1(sysUser.getPassword());
        sysUser.setPassword(password);
        sysUser.setCreateTime(new Date());
        boolean saveFlag = sysUser.save();
        if (saveFlag) {
            renderText(Constant.ADD_SUCCESS);
        } else {
            renderText(Constant.ADD_FAIL);
        }
    }

    public void updateAction() {
        SysUser sysUser = getBean(SysUser.class, "");
        String id = sysUser.getId();
        String password = sysUser.getPassword();
        // 认为密码改变
        if (password.length() < 20) {
            password = HashKit.sha1(password);   // 加密后为40位字符
            sysUser.setPassword(password);
        }
        sysUser.setCreateTime(new Date());
        boolean updateFlag = sysUser.update();
        if (updateFlag) {
            renderText(Constant.UPDATE_SUCCESS);
        } else {
            renderText(Constant.UPDATE_FAIL);
        }
    }

    @Before(Tx.class)
    public void deleteAction() {
        String id = getPara("id");
        // 用户表
        String deleteSql = "delete from sys_user where id = ?";
        Db.update(deleteSql, id);
        //用户角色表
        deleteSql = "delete from sys_user_role where user_id = ?";
        Db.update(deleteSql, id);
        renderText(Constant.DELETE_SUCCESS);
    }


    /**
     * 用户赋予角色
     */
    @Before(Tx.class)
    public void giveRole() {
        String userId = getPara("userId");
        String roleIdstr = getPara("roleIds");
        String deleteSql = "delete from  sys_user_role where user_id = ?";
        Db.update(deleteSql, userId);
        String[] roleIds = roleIdstr.split(";");
        for (int i = 0; i < roleIds.length; i++) {
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(userId);
            sysUserRole.setRoleId(Integer.parseInt(roleIds[i]));
            sysUserRole.save();
        }
        renderText("赋予角色成功");

    }


    /**
     * 全部菜单树，并根据角色选中
     */
    public void roleListChecked() {
        String id = getPara(0);
        List<SysUserRole> sysUserRoles = SysUserRole.dao.findUserRolesByUserId(id);
        List<SysRole> sysRoles = SysRole.dao.findAll();
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (SysRole sysRole : sysRoles) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", sysRole.getId());
            map.put("text", sysRole.getRoleName());
            map.put("state", "open");
            map.put("iconCls", "icon-blank");  // 不显示图标
            for (SysUserRole sysUserRole : sysUserRoles) {
                if (sysUserRole.getRoleId() == sysRole.getId()) {
                    map.put("checked", true);
                    break;
                }
            }
            maps.add(map);
        }
        renderJson(maps);
    }


}
