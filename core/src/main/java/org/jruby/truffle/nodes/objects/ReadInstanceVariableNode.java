/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class ReadInstanceVariableNode extends RubyNode implements ReadNode {

    @Child protected RubyNode receiver;
    @Child protected ReadHeadObjectFieldNode readNode;
    private final boolean isGlobal;

    private final BranchProfile nullProfile = BranchProfile.create();
    private final BranchProfile primitiveProfile = BranchProfile.create();

    public ReadInstanceVariableNode(RubyContext context, SourceSection sourceSection, Object name, RubyNode receiver, boolean isGlobal) {
        super(context, sourceSection);
        this.receiver = receiver;
        readNode = new ReadHeadObjectFieldNode(name);
        this.isGlobal = isGlobal;
    }

    @Override
    public int executeIntegerFixnum(VirtualFrame frame) throws UnexpectedResultException {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof RubyBasicObject) {
            return readNode.executeInteger((RubyBasicObject) receiverObject);
        } else {
            // TODO(CS): need to put this onto the fast path?

            CompilerDirectives.transferToInterpreter();
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        }
    }

    @Override
    public long executeLongFixnum(VirtualFrame frame) throws UnexpectedResultException {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof RubyBasicObject) {
            return readNode.executeLong((RubyBasicObject) receiverObject);
        } else {
            // TODO(CS): need to put this onto the fast path?

            CompilerDirectives.transferToInterpreter();
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        }
    }

    @Override
    public double executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof RubyBasicObject) {
            return readNode.executeDouble((RubyBasicObject) receiverObject);
        } else {
            // TODO(CS): need to put this onto the fast path?

            CompilerDirectives.transferToInterpreter();
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof RubyBasicObject) {
            Object value = readNode.execute((RubyBasicObject) receiverObject);

            if (value == null) {
                nullProfile.enter();
                value = getContext().getCoreLibrary().getNilObject();
            }

            return value;
        } else {
            primitiveProfile.enter();
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        notDesignedForCompilation();

        if (isGlobal) {
            final RubyBasicObject recieverValue = (RubyBasicObject) receiver.execute(frame);

            if (readNode.getName().equals("$~") || readNode.getName().equals("$!")) {
                return getContext().makeString("global-variable");
            } else if (readNode.isSet(recieverValue)) {
                if (readNode.execute(recieverValue) == getContext().getCoreLibrary().getNilObject()) {
                    return getContext().getCoreLibrary().getNilObject();
                } else {
                    return getContext().makeString("global-variable");
                }
            } else {
                return getContext().getCoreLibrary().getNilObject();
            }
        }

        final RubyContext context = getContext();

        try {
            final Object receiverObject = receiver.execute(frame);

            if (receiverObject instanceof RubyBasicObject) {
                final RubyBasicObject receiverRubyObject = (RubyBasicObject) receiverObject;

                final Shape layout = receiverRubyObject.getDynamicObject().getShape();
                final Property storageLocation = layout.getProperty(readNode.getName());

                if (storageLocation != null) {
                    return context.makeString("instance-variable");
                } else {
                    return getContext().getCoreLibrary().getNilObject();
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @Override
    public RubyNode makeWriteNode(RubyNode rhs) {
        return new WriteInstanceVariableNode(getContext(), getSourceSection(), readNode.getName(), receiver, rhs, isGlobal);
    }
}
