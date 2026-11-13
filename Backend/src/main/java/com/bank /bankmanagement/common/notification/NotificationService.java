package com.bank.bankmanagement.common.notification;
import com.bank.bankmanagement.user.entity.BankUser;import org.springframework.data.domain.*;import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;
@Service public class NotificationService{
 private final NotificationRepository repository;public NotificationService(NotificationRepository repository){this.repository=repository;}
 @Transactional public void notifyUser(BankUser user,String title,String message){if(user!=null&&title!=null&&message!=null)repository.save(new BankNotification(user,title,message));}
 @Transactional(readOnly=true) public Page<NotificationResponse> list(String email,Long userId,int page,int size){validate(page,size);return repository.findByUser_IdOrderByCreatedAtDesc(userId,PageRequest.of(page,size)).map(NotificationResponse::from);}
 @Transactional public NotificationResponse markRead(Long id,Long userId){BankNotification n=repository.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));n.markRead();return NotificationResponse.from(n);}
 private void validate(int p,int s){if(p<0||s<1||s>100)throw new IllegalArgumentException("Page must be >= 0 and size 1 to 100");}
}
