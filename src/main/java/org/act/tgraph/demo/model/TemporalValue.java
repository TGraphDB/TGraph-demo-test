package org.act.tgraph.demo.model;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.act.temporalProperty.exception.TPSRuntimeException;
import org.act.temporalProperty.query.TimeInterval;
import org.act.temporalProperty.query.TimePointL;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Created by song on 2018-05-09.
 * 用于表示一个时态值：（时间区间，值）元组构成的序列，时间区间不相交
 * 实现时用的是（时间点，值）组成的TreeMap，注意其中时间点不含有TimePoint.NOW，但可以有TimePoint.Init
 * TemporalValue只考虑值是否是Unknown，不考虑Invalid的问题（Invalid也算正常值）。
 */
public class TemporalValue<V>
{
    private final TreeMap<TimePointL, ValWithFlag> map = new TreeMap<>();

    public TemporalValue(V initialValue)
    {
        map.put( TimePointL.Init, val( initialValue ) );
    }

    public TemporalValue()
    {
    }

    public void put( TimeInterval interval, V value )
    {
        if ( interval.end().isNow() )
        {
            map.tailMap( interval.start(), true ).clear();
            map.put( interval.start(), val( value ) );
        }else{
            ValWithFlag end = map.get( interval.end().next() );
            map.subMap( interval.start(), true, interval.end(), true ).clear();
            map.put( interval.start(), val( value ) );
            if ( end == null )
            {
                map.put( interval.end().next(), valUnknown() );
            }
            else
            {
                map.put( interval.end().next(), end );
            }
        }
    }

    public V get( TimePointL time )
    {
        Entry<TimePointL, ValWithFlag> entry = map.floorEntry( time );
        if(entry!=null)
        {
            ValWithFlag val = entry.getValue();
            if ( val == null || val.isUnknown ) { return null; }
            else {
                return val.value;
            }
        }else{
            return null;
        }
    }

    private ValWithFlag valUnknown()
    {
        return new ValWithFlag( true, null );
    }

    private ValWithFlag val(V value )
    {
        return new ValWithFlag( false, value );
    }

    public boolean overlap( TimePointL startTime, TimePointL endTime )
    {
        Entry<TimePointL, ValWithFlag> floor = map.floorEntry( endTime );
        if ( floor == null )
        {
            return false;
        }
        else if ( floor.getValue().isUnknown )
        {
            return (floor.getKey().compareTo( startTime ) >= 0);
        }
        else
        {
            return true;
        }
    }

    public TimeInterval covered()
    {
        if ( map.size() > 2 )
        {
            return new TimeInterval( map.firstKey(), map.lastKey().pre() );
        }
        else if ( map.size() == 1 )
        {
            assert map.firstKey().isInit();
            return new TimeInterval( map.firstKey(), TimePointL.Now );
        }
        else
        {
            throw new TPSRuntimeException( "contain no value!" );
        }
    }

    public PeekingIterator<Entry<TimeInterval,V>> intervalEntries()
    {
        return new IntervalIterator();
    }

    public PeekingIterator<Entry<TimeInterval,V>> intervalEntries( TimePointL start, TimePointL end )
    {
        return new AddIntervalIterator( start, end );
    }

    public PeekingIterator<Triple<TimePointL,Boolean,V>> pointEntries()
    {
        return Iterators.peekingIterator( Iterators.transform( map.entrySet().iterator(),
                                                               input -> Triple.of( input.getKey(), input.getValue().isUnknown, input.getValue().value ) ) );
    }

    public PeekingIterator<Triple<TimePointL,Boolean,V>> pointEntries( TimePointL startTime )
    {
        Entry<TimePointL, ValWithFlag> floor = map.floorEntry( startTime );
        if ( floor != null )
        {
            return Iterators.peekingIterator( Iterators.transform( map.tailMap( floor.getKey(), true ).entrySet().iterator(),
                    item -> Triple.of(Objects.requireNonNull(item).getKey(), item.getValue().isUnknown, item.getValue().value)) );
        } else {
            return Iterators.peekingIterator(new Iterator<Triple<TimePointL, Boolean, V>>() {
                @Override public boolean hasNext() { return false; }
                @Override public Triple<TimePointL, Boolean, V> next() { return null; }
            });
        }
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public TemporalValue<V> slice(TimePointL min, boolean includeMin, V max, boolean includeMax )
    {

        return null;
    }

    private class ValWithFlag
    {
        private boolean isUnknown;
        private V value;

        ValWithFlag(boolean isUnknown, V value )
        {
            this.isUnknown = isUnknown;
            this.value = value;
        }
    }

    private class TimeIntervalValueEntry implements Entry<TimeInterval,V>
    {

        private final TimeInterval key;
        private final V val;

        public TimeIntervalValueEntry( TimeInterval key, V val )
        {
            this.key = key;
            this.val = val;
        }

        @Override
        public TimeInterval getKey()
        {
            return key;
        }

        @Override
        public V getValue()
        {
            return val;
        }

        @Override
        public V setValue( V value )
        {
            throw new UnsupportedOperationException();
        }
    }

    private class IntervalIterator extends AbstractIterator<Entry<TimeInterval,V>> implements PeekingIterator<Entry<TimeInterval,V>>
    {
        PeekingIterator<Entry<TimePointL, ValWithFlag>> iterator = Iterators.peekingIterator( map.entrySet().iterator() );

        @Override
        protected Entry<TimeInterval,V> computeNext()
        {
            while ( iterator.hasNext() )
            {
                if ( !isUnknown( iterator.peek() ) )
                {
                    Entry<TimePointL, ValWithFlag> start = iterator.next();
                    if ( iterator.hasNext() )
                    {
                        Entry<TimePointL, ValWithFlag> endNext = iterator.peek();
                        return new TimeIntervalValueEntry( new TimeInterval( start.getKey(), endNext.getKey().pre() ), start.getValue().value );
                    }
                    else
                    {
                        return new TimeIntervalValueEntry( new TimeInterval( start.getKey(), TimePointL.Now ), start.getValue().value );
                    }
                }
                else
                {
                    iterator.next();
                }
            }
            return endOfData();
        }

        private boolean isUnknown( Entry<TimePointL, ValWithFlag> entry )
        {
            return entry.getValue().isUnknown;
        }
    }

    private class AddIntervalIterator extends AbstractIterator<Entry<TimeInterval,V>> implements PeekingIterator<Entry<TimeInterval,V>>
    {
        private final TimePointL end;
        private TimeIntervalValueEntry start;
        PeekingIterator<Entry<TimePointL, ValWithFlag>> iterator;

        public AddIntervalIterator( TimePointL start, TimePointL end )
        {
            this.end = end;
            this.iterator = Iterators.peekingIterator( map.subMap( start, end ).entrySet().iterator() );
            Entry<TimePointL, ValWithFlag> floorEntry = map.floorEntry( start );
            if ( !floorEntry.getValue().isUnknown && floorEntry.getKey().compareTo( start ) < 0 )
            {
                this.start = new TimeIntervalValueEntry( new TimeInterval( start, floorEntry.getKey().pre() ), floorEntry.getValue().value );
            }
        }

        @Override
        protected Entry<TimeInterval,V> computeNext()
        {
            if ( start != null )
            {
                TimeIntervalValueEntry tmp = start;
                start = null;
                return tmp;
            }

            while ( iterator.hasNext() )
            {
                if ( !isUnknown( iterator.peek() ) )
                {
                    Entry<TimePointL, ValWithFlag> start = iterator.next();
                    if ( iterator.hasNext() )
                    {
                        Entry<TimePointL, ValWithFlag> end = iterator.peek();
                        return new TimeIntervalValueEntry( new TimeInterval( start.getKey(), end.getKey() ), start.getValue().value );
                    }
                    else
                    {
                        return new TimeIntervalValueEntry( new TimeInterval( start.getKey(), this.end ), start.getValue().value );
                    }
                }
            }
            return endOfData();
        }

        private boolean isUnknown( Entry<TimePointL, ValWithFlag> entry )
        {
            return entry.getValue().isUnknown;
        }
    }
}
