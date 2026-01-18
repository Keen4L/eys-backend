package com.eys.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.admin.service.AdminStatsService;
import com.eys.mapper.CfgRoleMapper;
import com.eys.mapper.SysUserMapper;
import com.eys.mapper.SysUserMatchMapper;
import com.eys.model.vo.LeaderboardVO;
import com.eys.model.vo.UserStatsVO;
import com.eys.model.entity.CfgRole;
import com.eys.model.entity.SysUser;
import com.eys.model.entity.SysUserMatch;
import com.eys.model.enums.CampType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final SysUserMatchMapper sysUserMatchMapper;
    private final SysUserMapper sysUserMapper;
    private final CfgRoleMapper cfgRoleMapper;

    @Override
    public List<LeaderboardVO> getLeaderboard(int limit) {
        List<SysUserMatch> matches = sysUserMatchMapper.selectList(null);
        return buildLeaderboard(matches, limit);
    }

    @Override
    public List<LeaderboardVO> getLeaderboardByCamp(String campType, int limit) {
        List<SysUserMatch> matches = sysUserMatchMapper.selectList(
                new LambdaQueryWrapper<SysUserMatch>()
                        .eq(SysUserMatch::getCampType, CampType.valueOf(campType)));
        return buildLeaderboard(matches, limit);
    }

    @Override
    public List<LeaderboardVO> getLeaderboardByRole(Long roleId, int limit) {
        List<SysUserMatch> matches = sysUserMatchMapper.selectList(
                new LambdaQueryWrapper<SysUserMatch>().eq(SysUserMatch::getRoleId, roleId));
        return buildLeaderboard(matches, limit);
    }

    @Override
    public UserStatsVO getUserStats(Long userId) {
        List<SysUserMatch> matches = sysUserMatchMapper.selectList(
                new LambdaQueryWrapper<SysUserMatch>().eq(SysUserMatch::getUserId, userId));

        SysUser user = sysUserMapper.selectById(userId);
        UserStatsVO vo = new UserStatsVO();
        vo.setUserId(userId);
        vo.setNickname(user != null ? user.getNickname() : null);
        vo.setTotalGames(matches.size());
        vo.setWinGames((int) matches.stream().filter(SysUserMatch::getIsWinner).count());
        vo.setWinRate(vo.getTotalGames() > 0 ? vo.getWinGames() * 100.0 / vo.getTotalGames() : 0);
        vo.setCampStats(buildCampStats(matches));
        vo.setRoleStats(buildRoleStats(matches));
        return vo;
    }

    private List<LeaderboardVO> buildLeaderboard(List<SysUserMatch> matches, int limit) {
        Map<Long, List<SysUserMatch>> byUser = matches.stream()
                .collect(Collectors.groupingBy(SysUserMatch::getUserId));

        List<LeaderboardVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<SysUserMatch>> entry : byUser.entrySet()) {
            Long userId = entry.getKey();
            List<SysUserMatch> userMatches = entry.getValue();

            LeaderboardVO vo = new LeaderboardVO();
            vo.setUserId(userId);
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatarUrl(user.getAvatarUrl());
            }
            vo.setTotalGames(userMatches.size());
            vo.setWinGames((int) userMatches.stream().filter(SysUserMatch::getIsWinner).count());
            vo.setWinRate(vo.getTotalGames() > 0 ? vo.getWinGames() * 100.0 / vo.getTotalGames() : 0);
            result.add(vo);
        }

        result.sort((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()));
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private List<UserStatsVO.CampStats> buildCampStats(List<SysUserMatch> matches) {
        Map<CampType, List<SysUserMatch>> byCamp = matches.stream()
                .filter(m -> m.getCampType() != null)
                .collect(Collectors.groupingBy(SysUserMatch::getCampType));

        List<UserStatsVO.CampStats> result = new ArrayList<>();
        for (Map.Entry<CampType, List<SysUserMatch>> entry : byCamp.entrySet()) {
            UserStatsVO.CampStats cs = new UserStatsVO.CampStats();
            cs.setCampType(entry.getKey().getCode());
            cs.setGames(entry.getValue().size());
            cs.setWins((int) entry.getValue().stream().filter(SysUserMatch::getIsWinner).count());
            cs.setWinRate(cs.getGames() > 0 ? cs.getWins() * 100.0 / cs.getGames() : 0);
            result.add(cs);
        }
        return result;
    }

    private List<UserStatsVO.RoleStats> buildRoleStats(List<SysUserMatch> matches) {
        Map<Long, List<SysUserMatch>> byRole = matches.stream()
                .filter(m -> m.getRoleId() != null)
                .collect(Collectors.groupingBy(SysUserMatch::getRoleId));

        List<UserStatsVO.RoleStats> result = new ArrayList<>();
        for (Map.Entry<Long, List<SysUserMatch>> entry : byRole.entrySet()) {
            UserStatsVO.RoleStats rs = new UserStatsVO.RoleStats();
            rs.setRoleId(entry.getKey());
            CfgRole role = cfgRoleMapper.selectById(entry.getKey());
            if (role != null) {
                rs.setRoleName(role.getName());
            }
            rs.setGames(entry.getValue().size());
            rs.setWins((int) entry.getValue().stream().filter(SysUserMatch::getIsWinner).count());
            rs.setWinRate(rs.getGames() > 0 ? rs.getWins() * 100.0 / rs.getGames() : 0);
            result.add(rs);
        }
        return result;
    }
}
