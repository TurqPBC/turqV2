package io.turq.turq.service.impl;

import io.turq.turq.config.JwtTokenUtil;
import io.turq.turq.contstants.APIErrors;
import io.turq.turq.entities.ContestEntity;
import io.turq.turq.entities.LegislationEntity;
import io.turq.turq.entities.UserEntity;
import io.turq.turq.exceptions.ContestNotFoundException;
import io.turq.turq.exceptions.UnauthorizedException;
import io.turq.turq.model.contest.ContestRequest;
import io.turq.turq.repository.ContestRepository;
import io.turq.turq.service.interfaces.IContestService;
import io.turq.turq.service.interfaces.ILegislationService;
import io.turq.turq.service.interfaces.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ContestService implements IContestService {

    @Autowired
    private ContestRepository repository;

    @Autowired
    private IUserService userService;

    @Autowired
    private ILegislationService legislationService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public List<ContestEntity> findAll() {
        List<ContestEntity> contest = (List<ContestEntity>) repository.findAll();
        return contest;
    }

    @Override
    public ContestEntity findById(long id) {
        Optional<ContestEntity> contest = repository.findById(id);
        if (contest.isEmpty()) {
            throw new ContestNotFoundException(APIErrors.CONTEST_NOT_FOUND);
        }
        return contest.get();
    }
    @Override
    public List<LegislationEntity> getLegislationByContest(long contestId) {
        ContestEntity contest = this.findById(contestId);
        if (contest == null) {
            throw new ContestNotFoundException(APIErrors.CONTEST_NOT_FOUND);
        }
        List<LegislationEntity> legislation = legislationService.findByContest(contestId);
        return legislation;
    }

    @Override
    public ContestEntity createContest(ContestRequest req, String token) {
        ContestEntity retContest = null;
        String authorEmail = jwtTokenUtil.getSubject(token);
        try {
            UserEntity author = userService.findByEmail(authorEmail);
            ContestEntity contest = new ContestEntity(req.getTitle(), req.getEndDate(), req.getPrize(), req.getRules(), req.getCriteria(), false, req.getDescription(), author);
            retContest = repository.save(contest);
        } catch (DataIntegrityViolationException e){
            System.out.println("PSQL Data Integrity Violation Exception: " + e.getMessage());
            retContest = null;
        }
        return retContest;
    }

    @Override
    public ContestEntity updateContest(ContestRequest req, String token, long id) {
        ContestEntity retContest = null;
        String authorEmail = jwtTokenUtil.getSubject(token);
        try {
            ContestEntity contest = this.findById(id);
            if (contest == null) {
                throw new ContestNotFoundException(APIErrors.CONTEST_NOT_FOUND);
            } else if (!contest.getAuthor().getEmail().equalsIgnoreCase(authorEmail)) {
                throw new UnauthorizedException(APIErrors.CONTEST_UPDATE_PERMISSION);
            } else {
                retContest = repository.save(new ContestEntity(contest.getId(), req.getTitle(), req.getEndDate(), req.getPrize(), req.getRules(), req.getCriteria(), false, req.getDescription(), contest.getAuthor()));
            }
        } catch (DataIntegrityViolationException e){
            System.out.println("PSQL Data Integrity Violation Exception: " + e.getMessage());
            retContest = null;
        }
        return retContest;
    }

    @Override
    public void deleteContest(String token, long id) {
        String authorEmail = jwtTokenUtil.getSubject(token);
        try {
            ContestEntity contest = this.findById(id);
            if (contest == null) {
                throw new ContestNotFoundException(APIErrors.CONTEST_NOT_FOUND);
            } else if (!contest.getAuthor().getEmail().equalsIgnoreCase(authorEmail)) {
                throw new UnauthorizedException(APIErrors.CONTEST_UPDATE_PERMISSION);
            } else {
                repository.delete(contest);
            }
        } catch (DataIntegrityViolationException e){
            System.out.println("PSQL Data Integrity Violation Exception: " + e.getMessage());
        }
        return;
    }
}
