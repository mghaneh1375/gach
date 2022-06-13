package irysc.gachesefid.Security;

import irysc.gachesefid.Models.Role;
import org.bson.Document;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;


@Service
public class MyUserDetails implements UserDetailsService {

    private static HashMap<String, Document> cached = new HashMap<>();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Document user = cached.get(username);

        if(user == null) {
            user = userRepository.findByUsername(username);
            if (user == null)
                throw new UsernameNotFoundException("User '" + username + "' not found");
            else
                cached.put(username, user);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .authorities(Role.ROLE_CLIENT)
                .password(user.getString("password"))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();

    }

}