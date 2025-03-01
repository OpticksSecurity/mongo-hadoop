/*
 * Copyright 2010-2013 10gen Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.hadoop.input;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.hadoop.util.Bits;
import com.mongodb.hadoop.util.MongoConfigUtil;
import com.mongodb.util.JSON;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.bson.BSONCallback;
import org.bson.BSONDecoder;
import org.bson.BSONEncoder;
import org.bson.BSONObject;
import org.bson.BasicBSONCallback;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

public class MongoInputSplit extends InputSplit implements Writable, org.apache.hadoop.mapred.InputSplit {
    private static final Log LOG = LogFactory.getLog(MongoInputSplit.class);
    //CHECKSTYLE:OFF
    protected MongoClientURI inputURI;
    protected MongoClientURI authURI;
    protected String keyField = "_id";
    protected DBObject fields;
    protected DBObject query;
    protected DBObject sort;
    protected DBObject min;
    protected DBObject max;
    protected Integer limit;
    protected Integer skip;
    protected boolean notimeout = false;
    protected transient DBCursor cursor;

    protected transient BSONEncoder _bsonEncoder = new BasicBSONEncoder();
    protected transient BSONDecoder _bsonDecoder = new BasicBSONDecoder();
    //CHECKSTYLE:ON

    public MongoInputSplit() {
    }

    public MongoInputSplit(final MongoInputSplit other) {
        setFields(other.getFields());
        setAuthURI(other.getAuthURI());
        setInputURI(other.getInputURI());
        setKeyField(other.getKeyField());
        setMax(other.getMax());
        setMin(other.getMin());
        setNoTimeout(other.getNoTimeout());
        setQuery(other.getQuery());
        setSort(other.getSort());
        setLimit(other.getLimit());
        setSkip(other.getSkip());
    }

    public MongoInputSplit(final Configuration conf) {
        setFields(MongoConfigUtil.getFields(conf));
        setAuthURI(MongoConfigUtil.getAuthURI(conf));
        setInputURI(MongoConfigUtil.getInputURI(conf));
        setKeyField(MongoConfigUtil.getInputKey(conf));
        setMax(MongoConfigUtil.getMaxSplitKey(conf));
        setMin(MongoConfigUtil.getMinSplitKey(conf));
        setNoTimeout(MongoConfigUtil.isNoTimeout(conf));
        setQuery(MongoConfigUtil.getQuery(conf));
        setSort(MongoConfigUtil.getSort(conf));
        setLimit(MongoConfigUtil.getLimit(conf));
        setSkip(MongoConfigUtil.getSkip(conf));
    }

    public void setInputURI(final MongoClientURI inputURI) {
        this.inputURI = inputURI;
    }

    public MongoClientURI getInputURI() {
        return this.inputURI;
    }

    /**
     * Set the MongoDB URI to use for authentication.
     * @param authURI the MongoDB URI
     */
    public void setAuthURI(final MongoClientURI authURI) {
        this.authURI = authURI;
    }

    public MongoClientURI getAuthURI() {
        return this.authURI;
    }

    @Override
    public String[] getLocations() {
        if (this.inputURI == null) {
            return null;
        }
        return this.inputURI.getHosts().toArray(new String[inputURI.getHosts().size()]);
    }

    @Override
    public long getLength() {
        return Long.MAX_VALUE;
    }

    public String getKeyField() {
        return this.keyField;
    }

    public void setKeyField(final String keyField) {
        this.keyField = keyField;
    }

    public DBObject getFields() {
        return this.fields;
    }

    public void setFields(final DBObject fields) {
        this.fields = fields;
    }

    public DBObject getQuery() {
        return this.query;
    }

    public void setQuery(final DBObject query) {
        this.query = query;
    }

    public DBObject getSort() {
        return this.sort;
    }

    public void setSort(final DBObject sort) {
        this.sort = sort;
    }

    public DBObject getMin() {
        return this.min;
    }

    public void setMin(final DBObject min) {
        this.min = min;
    }

    public DBObject getMax() {
        return this.max;
    }

    public void setMax(final DBObject max) {
        this.max = max;
    }

    public boolean getNoTimeout() {
        return this.notimeout;
    }

    public void setNoTimeout(final boolean notimeout) {
        this.notimeout = notimeout;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(final Integer limit) {
        this.limit = limit;
    }

    public Integer getSkip() {
        return skip;
    }

    public void setSkip(final Integer skip) {
        this.skip = skip;
    }

    @Override
    public void write(final DataOutput out) throws IOException {
        BSONObject spec = BasicDBObjectBuilder.start()
          .add("inputURI", getInputURI().toString())
          .add("authURI", getAuthURI() != null ? getAuthURI().toString() : null)
          .add("keyField", getKeyField())
          .add("fields", getFields())
          .add("query", getQuery())
          .add("sort", getSort())
          .add("min", getMin())
          .add("max", getMax())
          .add("notimeout", getNoTimeout())
          .add("limit", limit)
          .add("skip", skip)
          .get();
        byte[] buf = _bsonEncoder.encode(spec);
        out.write(buf);
    }

    @Override
    public void readFields(final DataInput in) throws IOException {
        BSONCallback cb = new BasicBSONCallback();
        BSONObject spec;
        byte[] l = new byte[4];
        in.readFully(l);
        int dataLen = Bits.readInt(l);
        byte[] data = new byte[dataLen + 4];
        System.arraycopy(l, 0, data, 0, 4);
        in.readFully(data, 4, dataLen - 4);
        _bsonDecoder.decode(data, cb);
        spec = (BSONObject) cb.get();
        setInputURI(new MongoClientURI((String) spec.get("inputURI")));

        if (spec.get("authURI") != null) {
            setAuthURI(new MongoClientURI((String) spec.get("authURI")));
        } else {
            setAuthURI((MongoClientURI) null);
        }

        setKeyField((String) spec.get("keyField"));
        BSONObject temp = (BSONObject) spec.get("fields");
        setFields(temp != null ? new BasicDBObject(temp.toMap()) : null);

        temp = (BSONObject) spec.get("query");
        setQuery(temp != null ? new BasicDBObject(temp.toMap()) : null);

        temp = (BSONObject) spec.get("sort");
        setSort(temp != null ? new BasicDBObject(temp.toMap()) : null);

        temp = (BSONObject) spec.get("min");
        setMin(temp != null ? new BasicDBObject(temp.toMap()) : null);

        temp = (BSONObject) spec.get("max");
        setMax(temp != null ? new BasicDBObject(temp.toMap()) : null);

        setLimit((Integer) spec.get("limit"));

        setSkip((Integer) spec.get("skip"));

        setNoTimeout((Boolean) spec.get("notimeout"));
    }

    public DBCursor getCursor() {
        if (this.cursor == null) {
            DBCollection coll;
            if (this.authURI != null) {
                coll = MongoConfigUtil.getCollectionWithAuth(this.inputURI, this.authURI);
            } else {
                coll = MongoConfigUtil.getCollection(this.inputURI);
            }

            this.cursor = coll.find(this.query, this.fields).sort(this.sort);
            if (this.notimeout) {
                this.cursor.noCursorTimeout(true);
            }
            DBObject bound = null;
            if (this.min != null && !this.min.keySet().isEmpty())
                bound = this.min;
            else if (this.max != null && !this.max.keySet().isEmpty())
                bound = this.max;
            if (bound != null) {
                Set<String> boundFields = bound.keySet();

                String indexName = null;
                for (DBObject index : coll.getIndexInfo()) {
                    //try {
                        DBObject key = (DBObject) index.get("key");
                        Set<String> indexFields = key.keySet();
                        if (indexFields.size() == boundFields.size() && indexFields.containsAll(boundFields)) {
                            indexName = (String) index.get("name");
                            break;
                        }
                    //} catch (Exception e) {}
                }
                if (indexName != null) {
                    this.cursor.hint(indexName);
                } else {
                    throw new MongoException("Failed to detect a suitable hint index for cursor min/max");
                }
            }
            if (this.min != null) {
                this.cursor.min(this.min);
            }
            if (this.max != null) {
                this.cursor.max(this.max);
            }
            if (skip != null) {
                cursor = cursor.skip(skip);
            }
            if (limit != null) {
                cursor = cursor.limit(limit);
            }
        }
        return this.cursor;
    }

    @Override
    public String toString() {
        String result =
          "MongoInputSplit{inputURI hosts=" + this.inputURI.getHosts()
            + ", inputURI namespace=" + this.inputURI.getDatabase() + "."
            + this.inputURI.getCollection();
        if (authURI != null) {
            result += "authURI hosts=" + authURI.getHosts()
              + ", authURI database=" + authURI.getDatabase();
        }
        return result
          + ", min=" + this.min + ", max=" + this.max
          + ", query=" + this.query
          + ", sort=" + this.sort
          + ", fields=" + this.fields
          + ", limit=" + this.limit
          + ", skip=" + this.skip
          + ", notimeout=" + this.notimeout + '}';
    }

    @Override
    public int hashCode() {
        int result = this.inputURI != null ? this.inputURI.hashCode() : 0;
        result = 31 * result + (this.query != null ? this.query.hashCode() : 0);
        result = 31 * result + (this.fields != null ? this.fields.hashCode() : 0);
        result = 31 * result + (this.max != null ? this.max.hashCode() : 0);
        result = 31 * result + (this.min != null ? this.min.hashCode() : 0);
        result = 31 * result + (this.sort != null ? this.sort.hashCode() : 0);
        result = 31 * result + (this.notimeout ? 1 : 0);
        result = 31 * result + (this.limit != null ? this.limit.hashCode() : 0);
        result = 31 * result + (this.skip != null ? this.skip.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MongoInputSplit that = (MongoInputSplit) o;

        if (getNoTimeout() != that.getNoTimeout()) {
            return false;
        }
        if (getFields() != null ? !getFields().equals(that.getFields()) : that.getFields() != null) {
            return false;
        }
        if (getInputURI() != null ? !getInputURI().equals(that.getInputURI()) : that.getInputURI() != null) {
            return false;
        }
        if (getQuery() != null ? !getQuery().equals(that.getQuery()) : that.getQuery() != null) {
            return false;
        }
        if (getSort() != null ? !getSort().equals(that.getSort()) : that.getSort() != null) {
            return false;
        }
        if (getMax() != null ? !getMax().equals(that.getMax()) : that.getMax() != null) {
            return false;
        }
        if (getMin() != null ? !getMin().equals(that.getMin()) : that.getMin() != null) {
            return false;
        }
        if (limit == null && that.getLimit() != null
          || !limit.equals(that.getLimit())) {
            return false;
        }
        if (skip == null && that.getSkip() != null
          || !skip.equals(that.getSkip())) {
            return false;
        }
        return true;
    }

}
