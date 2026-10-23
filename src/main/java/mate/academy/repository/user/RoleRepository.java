package mate.academy.repository.user;

import java.util.Optional;
import mate.academy.model.user.Role;
import mate.academy.model.user.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
