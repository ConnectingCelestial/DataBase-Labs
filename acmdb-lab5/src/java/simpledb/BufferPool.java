package simpledb;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /**
     * Bytes per page, including header.
     */
    private static final int PAGE_SIZE = 4096;

    private static int pageSize = PAGE_SIZE;

    /**
     * Default number of pages passed to the constructor. This is used by
     * other classes. BufferPool should use the numPages argument to the
     * constructor instead.
     */
    public static final int DEFAULT_PAGES = 50;

    private final int numPages;

    private final ConcurrentHashMap<PageId, Page> pages;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        this.numPages = numPages;
        pages = new ConcurrentHashMap<>();
        locks = new ConcurrentHashMap<>();
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, an page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid  the ID of the transaction requesting the page
     * @param pid  the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
        // some code goes here
        long startTime = System.currentTimeMillis();
        try {
            //申请加锁
            while (!requireLock(tid, pid, perm)) {
                long curTime = System.currentTimeMillis();
                //避免死锁策略：超时中断
                if (curTime - startTime > 500)
                    throw new TransactionAbortedException();
            }

            //若缓冲池有该页，返回；无该页，从磁盘读取
            if (pages.containsKey(pid)) {
                return pages.get(pid);
            } else {
                if (pages.size() >= numPages)
                    evictPage();
                Page page = Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);
                pages.put(pid, page);
                return page;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private final ConcurrentHashMap<PageId, ConcurrentHashMap<TransactionId, LockType>> locks;

    protected enum LockType {
        SHARE, EXCLUSIVE
    }

    public synchronized boolean requireLock(TransactionId tid, PageId pid, Permissions perm) {
        ConcurrentHashMap<TransactionId, LockType> pageLocks;
        //perm = Permissions.READ_WRITE;
        //若该页无锁，直接上锁
        if (!locks.containsKey(pid)) {
            pageLocks = new ConcurrentHashMap<>();
            if (perm == Permissions.READ_ONLY)
                pageLocks.put(tid, LockType.SHARE);
            else pageLocks.put(tid, LockType.EXCLUSIVE);
            locks.put(pid, pageLocks);
            return true;
        }
        pageLocks = locks.get(pid);
        for (TransactionId id : pageLocks.keySet()) {
            //若有一读锁，则全为读锁
            if (pageLocks.get(id) == LockType.SHARE)
                //上读锁成功
                if (perm == Permissions.READ_ONLY) {
                    pageLocks.put(tid, LockType.SHARE);
                    return true;
                }
                //上写锁
                else {  //仅有一锁且持有者为tid，升级，否则加锁失败
                    if (pageLocks.size() == 1 && id.equals(tid)) {
                        pageLocks.put(tid, LockType.EXCLUSIVE);
                        return true;
                    }
                    else return false;
                }
            //有一写锁，若持有者为tid，返回成功，否则加锁失败
            else return id.equals(tid);
        }
        return false;
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public synchronized void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
        if (locks.containsKey(pid)) {
            locks.get(pid).remove(tid);
            if (locks.get(pid).size() == 0)
                locks.remove(pid);
        }
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public synchronized void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        transactionComplete(tid, true);
    }

    /**
     * Return true if the specified transaction has a lock on the specified page
     */
    public synchronized boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        return locks.get(p).containsKey(tid);
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid    the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public synchronized void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        for (PageId pid : locks.keySet()) {
            ConcurrentHashMap<TransactionId, LockType> pageLocks = locks.get(pid);
            if (pageLocks.containsKey(tid)) {
                if (pageLocks.get(tid) == LockType.EXCLUSIVE && pages.containsKey(pid)) {
                    if (commit) flushPage(pid);
                    else discardPage(pid);
                }
                releasePage(tid, pid);
            }
        }
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid     the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t       the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        DbFile bf = Database.getCatalog().getDatabaseFile(tableId);
        ArrayList<Page> dirtyPages = new ArrayList<>();
        //handle stupid HeapFileDuplicates测试
        if (bf instanceof BufferPoolWriteTest.HeapFileDuplicates)
            for (int i = 0; i < 10; i++) {
                HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, new HeapPageId(bf.getId(), i), Permissions.READ_WRITE);
                page.insertTuple(t);
                dirtyPages.add(page);
            }
        else dirtyPages = bf.insertTuple(tid, t);
        for (Page page : dirtyPages) {
            page.markDirty(true, tid);
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t   the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        int tableId = t.getRecordId().getPageId().getTableId();
        DbFile bf = Database.getCatalog().getDatabaseFile(tableId);
        ArrayList<Page> dirtyPages = bf.deleteTuple(tid, t);
        for (Page page : dirtyPages) {
            page.markDirty(true, tid);
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     * break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for (PageId pid : pages.keySet())
            flushPage(pid);
    }

    /**
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     * <p>
     * Also used by B+ tree files to ensure that deleted pages
     * are removed from the cache so they can be reused safely
     */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
        pages.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     *
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        Page page = pages.get(pid);
        Database.getCatalog().getDatabaseFile(pid.getTableId()).writePage(page);
        page.markDirty(false, null);
    }

    /**
     * Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        for (PageId pid : pages.keySet()) {
            if (tid == pages.get(pid).isDirty())
                flushPage(pid);
        }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException, IOException {
        // some code goes here
        // not necessary for lab1
        for (PageId pid : pages.keySet()) {
            //NO STEAL policy
            if (pages.get(pid).isDirty() == null) {
                discardPage(pid);
                return;
            }
        }
        throw new DbException("all pages in bufferPool are dirty");
    }

}