<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"  %>

<a href="/MyBlog">Home</a> 
<c:if test="${loginUser == null }">
| <a href="/MyBlog/user/add">Sign In</a>
| <a href="/MyBlog/user/login">Login</a>
</c:if>

<c:if test="${loginUser != null }">
| <a href="/MyBlog/blog/list">List Blog</a> 
| <a href="/MyBlog/blog/add">Post Blog</a> 
| Welcome 
<a href="/MyBlog/user/show/${loginUser.loginId }"> ${loginUser.loginId } </a>
</c:if>

<p />