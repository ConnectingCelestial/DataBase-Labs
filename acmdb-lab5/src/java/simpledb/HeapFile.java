package simpledb;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static simpledb.Permissions.READ_ONLY;
import static simpledb.Permissions.READ_WRITE;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see simpledb.HeapPage#HeapPage
 */
public class HeapFile implements DbFile {

    private final File f;

    private final TupleDesc td;

    private int numPages;

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap
     *          file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.f = f;
        this.td = td;
        Database.getCatalog().addTable(this);
        try {
            FileInputStream fis = new FileInputStream(f);
            numPages = Math.ceilDiv(fis.available(), BufferPool.getPageSize());
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        return f.getAbsoluteFile().hashCode() * 31 + td.hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return this.td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        if (pid.pageNumber() > numPages) return null;
        if (pid.pageNumber() == numPages)
            try {
                RandomAccessFile rf = new RandomAccessFile(f, "rw");
                rf.seek(BufferPool.getPageSize() * numPages);
                rf.write(HeapPage.createEmptyPageData());
                rf.close();
                numPages++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[BufferPool.getPageSize()];
            fis.skip(pid.pageNumber() * BufferPool.getPageSize());
            fis.read(data);
            fis.close();
            return new HeapPage((HeapPageId) pid, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
        try {
            PageId pid = page.getId();
            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[BufferPool.getPageSize() * Math.max(numPages, 1)];
            fis.read(data);
            fis.close();
            FileOutputStream fos = new FileOutputStream(f);
            byte[] pgData = page.getPageData();
            for (int i = BufferPool.getPageSize() * pid.pageNumber(), j = 0; j < BufferPool.getPageSize(); i++, j++)
                data[i] = pgData[j];
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        return numPages;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        for (int i = 0; i <= numPages; i++) {
            PageId pid = new HeapPageId(getId(), i);
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, READ_WRITE);
            if (page.getNumEmptySlots() != 0) {
                page.insertTuple(t);
                ArrayList<Page> arr = new ArrayList<>();
                arr.add(page);
                return arr;
            } else Database.getBufferPool().releasePage(tid, pid);//若无空slot立即释放锁
        }
        return null;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, t.getRecordId().getPageId(), READ_WRITE);
        page.deleteTuple(t);
        ArrayList<Page> arr = new ArrayList<>();
        arr.add(page);
        return arr;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new DbFileIterator() {

            private boolean isOpen = false;

            private int curPage = 0;

            private Iterator<Tuple> it;

            @Override
            public boolean hasNext() throws DbException, TransactionAbortedException {
                if (isOpen) {
                    if (it.hasNext())
                        return true;
                    else if (curPage >= numPages - 1)
                        return false;
                    else
                        return ((HeapPage) Database.getBufferPool().getPage(
                                tid, new HeapPageId(getId(), curPage + 1), READ_ONLY
                        )).iterator().hasNext();
                }
                return false;
            }

            @Override
            public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
                if (isOpen)
                    if (it.hasNext())
                        return it.next();
                    else if (curPage >= numPages - 1)
                        throw new NoSuchElementException();
                    else {
                        it = ((HeapPage) Database.getBufferPool().getPage(
                                tid, new HeapPageId(getId(), ++curPage), READ_ONLY
                        )).iterator();
                        return it.next();
                    }
                else throw new NoSuchElementException();
            }

            @Override
            public void open() throws DbException, TransactionAbortedException {
                isOpen = true;
                curPage = 0;
                HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, new HeapPageId(getId(), curPage), READ_ONLY);
                it = page.iterator();
            }

            @Override
            public void rewind() throws DbException, TransactionAbortedException {
                close();
                open();
            }

            @Override
            public void close() {
                isOpen = false;
                curPage = 0;
                it = null;
            }
        };
    }

}

