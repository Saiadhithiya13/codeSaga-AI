import React, { useState, useEffect, useRef } from 'react'
import { SemanticSearchResultDto } from '@/types/repository.types'
import { ChatSessionDto, ChatMessageDto } from '@/types/chat.types'
import { createChatSession, getSessionMessages } from '@/features/chat/api/chatApi'

interface Props {
  repositoryId: string;
}

// In a real app, use react-markdown to render the content
const MarkdownRenderer = ({ content }: { content: string }) => {
  return <pre className="whitespace-pre-wrap font-sans text-sm text-slate-300">{content}</pre>
}

export default function RepositoryChatPanel({ repositoryId }: Props) {
  const [session, setSession] = useState<ChatSessionDto | null>(null)
  const [messages, setMessages] = useState<ChatMessageDto[]>([])
  const [input, setInput] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [citations, setCitations] = useState<SemanticSearchResultDto[]>([])
  
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleCreateSession = async () => {
    try {
      const newSession = await createChatSession({
        repositoryId,
        title: 'New Chat Session'
      })
      setSession(newSession)
    } catch (err: any) {
      alert(err.message || 'Failed to create chat session')
    }
  }

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!input.trim() || isStreaming || !session) return

    const userMessageText = input.trim()
    setInput('')
    setCitations([]) // clear previous citations

    // Optimistically add user message
    const userMsg: ChatMessageDto = {
      id: crypto.randomUUID(),
      role: 'USER',
      content: userMessageText,
      createdAt: new Date().toISOString()
    }
    setMessages(prev => [...prev, userMsg])
    
    // Add empty assistant message to stream into
    const assistantMsgId = crypto.randomUUID()
    setMessages(prev => [...prev, {
      id: assistantMsgId,
      role: 'ASSISTANT',
      content: '',
      createdAt: new Date().toISOString()
    }])

    setIsStreaming(true)

    try {
      const response = await fetch(`/api/v1/chat/sessions/${session.id}/messages/stream`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ message: userMessageText })
      })

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          throw new Error('Your session has expired. Please log out and log back in.')
        }
        throw new Error(`Failed to send message: ${response.statusText}`)
      }

      if (!response.body) throw new Error('No response body')

      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      while (true) {
        const { value, done } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        // Process SSE events: each line is like `event: name\ndata: ...\n\n`
        // We will do a simple parsing here
        const lines = chunk.split('\n')
        let currentEvent = ''
        
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const dataStr = line.substring(5).trim()
            
            if (currentEvent === 'token') {
              setMessages(prev => prev.map(m => 
                m.id === assistantMsgId ? { ...m, content: m.content + dataStr } : m
              ))
            } else if (currentEvent === 'citations') {
              try {
                const parsedCitations = JSON.parse(dataStr) as SemanticSearchResultDto[]
                setCitations(parsedCitations)
              } catch (e) {}
            } else if (currentEvent === 'done') {
              setIsStreaming(false)
            } else if (currentEvent === 'error') {
              alert(`Error: ${dataStr}`)
              setIsStreaming(false)
            }
          }
        }
      }
    } catch (err: any) {
      console.error(err)
      // Set the assistant's message to show the error
      setMessages(prev => prev.map(m => 
        m.id === assistantMsgId ? { ...m, content: `⚠️ Error: ${err.message}` } : m
      ))
      setIsStreaming(false)
    }
  }

  if (!session) {
    return (
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 text-center">
        <div className="w-16 h-16 bg-violet-900/30 text-violet-400 rounded-2xl flex items-center justify-center mx-auto mb-4 border border-violet-500/20">
          <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
          </svg>
        </div>
        <h3 className="text-lg font-bold text-white mb-2">Repository Chat (RAG)</h3>
        <p className="text-slate-400 text-sm mb-6 max-w-md mx-auto">
          Ask questions about your codebase. CodeSage will search your indexed files and generate answers with precise source citations.
        </p>
        <button
          onClick={handleCreateSession}
          className="bg-violet-600 hover:bg-violet-500 text-white font-medium py-2 px-6 rounded-xl transition-colors shadow-lg shadow-violet-600/20"
        >
          Start New Chat Session
        </button>
      </div>
    )
  }

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl flex flex-col h-[600px] overflow-hidden">
      {/* Header */}
      <div className="px-6 py-4 border-b border-slate-800 bg-slate-900/50 flex justify-between items-center">
        <h3 className="text-sm font-semibold text-slate-300 flex items-center gap-2">
          <svg className="w-4 h-4 text-violet-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
          </svg>
          {session.title}
        </h3>
        <span className="text-xs text-slate-500 bg-slate-800 px-2 py-1 rounded">RAG Enabled</span>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {messages.length === 0 && (
          <div className="text-center text-slate-500 text-sm mt-10">
            Send a message to start chatting with your repository.
          </div>
        )}
        
        {messages.map(msg => (
          <div key={msg.id} className={`flex ${msg.role === 'USER' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[80%] rounded-2xl px-5 py-3 ${
              msg.role === 'USER' 
                ? 'bg-violet-600 text-white rounded-br-none' 
                : 'bg-slate-800 text-slate-300 border border-slate-700 rounded-bl-none'
            }`}>
              {msg.content === '' && msg.role === 'ASSISTANT' && isStreaming ? (
                <div className="flex gap-1 py-1 px-1">
                  <span className="thinking-dot text-slate-400"></span>
                  <span className="thinking-dot text-slate-400"></span>
                  <span className="thinking-dot text-slate-400"></span>
                </div>
              ) : (
                <MarkdownRenderer content={msg.content} />
              )}
            </div>
          </div>
        ))}

        {citations.length > 0 && !isStreaming && (
          <div className="flex justify-start">
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 max-w-[80%]">
              <p className="text-xs text-slate-500 uppercase tracking-wider font-medium mb-2">Sources Referenced</p>
              <div className="flex flex-wrap gap-2">
                {citations.map((cite, i) => (
                  <div key={i} className="text-xs bg-slate-900 border border-slate-700 px-2 py-1 rounded text-slate-400 flex items-center gap-2">
                    <span>📄 {cite.filePath}</span>
                    <span className="text-emerald-500">{(cite.similarityScore * 100).toFixed(0)}%</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="p-4 bg-slate-900/50 border-t border-slate-800">
        <form onSubmit={handleSend} className="relative">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={isStreaming}
            placeholder="Ask anything about your code..."
            className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-4 pr-12 py-3 text-white placeholder-slate-500 focus:outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={!input.trim() || isStreaming}
            className="absolute right-2 top-2 p-1.5 bg-violet-600 hover:bg-violet-500 text-white rounded-lg disabled:opacity-50 transition-colors"
          >
            {isStreaming ? (
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            )}
          </button>
        </form>
      </div>
    </div>
  )
}
