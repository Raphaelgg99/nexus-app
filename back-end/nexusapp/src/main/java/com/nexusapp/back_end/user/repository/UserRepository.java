package com.nexusapp.back_end.user.repository;

import com.nexusapp.back_end.user.model.User;
import com.nexusapp.back_end.user.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserNameIgnoreCase(String userName);

    boolean existsByUserNameIgnoreCase(String userName);

    boolean existsByUserNameIgnoreCaseAndIdNot(String userName, Long id);

    long countByRole(UserRole role);

    List<User> findAllByOrderByUserNameAsc();
}
