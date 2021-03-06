package cn.starrah.wxoabkd.users

import com.mongodb.client.MongoCollection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.Document
import cn.starrah.wxoabkd.utils.Saveable
import cn.starrah.wxoabkd.utils.ToObject

/**
 * 用户信息管理的泛型类定义。
 *
 * 用于实现从数据库中取出某一集合，利用它们的信息实例化用户对象和保存操作等，不提供其他功能。
 *
 * 使用方法：对于一个特定群体的用户，继承XXXUser类后，再定义单例XXXUsers: DBUsers<XXXUser>(collection, XXXUser::class.java)
 */
open class DBUsers <T: DBUser> (
    /** 用户信息对应的数据库集合 */ val collection: MongoCollection<Document>,
    /** 被泛型实例化的类对象 */clazz: Class<T>
): Users<T>(clazz), Saveable
{
    init {
         //延迟load的操作。若直接在init调用，子类的成员还没有来得及初始化，会抛出NPE
        GlobalScope.launch {
            delay(1000)
            load()
        }

    }

    override fun load() {
        _openIdMap.clear()
        _usersList.clear()
        for (document in collection.find()) {
            val user = document.ToObject(clazz)
            if(user != null) {
                _usersList.add(user)
                user.collection = collection
                user.openId?.let { _openIdMap.put(it, user) }
                user.load()
            }
        }
    }

    /**
     * 保存所有用户的数据，即对所有用户调用Save方法。
     */
    override fun save() {
        for(user in _usersList){
            user.save()
        }
    }

    override fun addUser(user: T) {
        super.addUser(user)
        user.save()
    }

    override fun removeUser(user: T): Boolean {
        val res = super.removeUser(user)
        if(res) {
            val doc: Document?
            if (user.openId != null) doc = Document().append("openId", user.openId)
            else if (user.name != null) doc = Document().append("name", user.name)
            else doc = null
            doc?.let { collection.deleteOne(it) }
        }
        return res
    }
}