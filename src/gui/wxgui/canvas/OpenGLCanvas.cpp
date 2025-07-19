<<<<<<< HEAD:src/gui/canvas/OpenGLCanvas.cpp
#include "canvas/OpenGLCanvas.h"
=======
#include "wxgui/canvas/OpenGLCanvas.h"

#include "wxgui/canvas/IRenderCanvas.h"

>>>>>>> public/main:src/gui/wxgui/canvas/OpenGLCanvas.cpp
#include "Cafe/HW/Latte/Renderer/OpenGL/OpenGLRenderer.h"

#include "config/CemuConfig.h"
#include "Cafe/HW/Latte/Renderer/OpenGL/GLCanvas.h"
#include "Common/GLInclude/GLInclude.h"
#include <wx/glcanvas.h> // this includes GL/gl.h, avoid using this in a header because it would contaminate our own OpenGL definitions (GLInclude)

static const int g_gl_attribute_list[] =
{
	WX_GL_RGBA,
	WX_GL_DOUBLEBUFFER,
	WX_GL_DEPTH_SIZE, 16,

	WX_GL_MIN_RED, 8,
	WX_GL_MIN_GREEN, 8,
	WX_GL_MIN_BLUE, 8,
	WX_GL_MIN_ALPHA, 8,

	WX_GL_STENCIL_SIZE, 8,

	//WX_GL_MAJOR_VERSION, 4,
	//WX_GL_MINOR_VERSION, 1,
	//wx_GL_COMPAT_PROFILE,

	0, // end of list
};

class OpenGLCanvas;

class GLCanvasManager : public OpenGLCanvasCallbacks
{
  public:
	GLCanvasManager()
	{
		SetOpenGLCanvasCallbacks(this);
	}

	~GLCanvasManager()
	{
		ClearOpenGLCanvasCallbacks();
	}

	void SetTVView(OpenGLCanvas* canvas)
	{
		m_tvView = canvas;
	}

	void SetPadView(OpenGLCanvas* canvas)
	{
		m_padView = canvas;
	}

	void SetGLContext(wxGLContext* context)
	{
		m_glContext = context;
	}

	void DeleteGLContext()
	{
		if (m_tvView == nullptr && m_padView == nullptr && m_glContext)
		{
			delete m_glContext;
			m_glContext = nullptr;
		}
	}

	bool HasPadViewOpen() const
	{
		return m_padView != nullptr;
	}

	bool MakeCurrent(bool padView);

	void SwapBuffers(bool swapTV, bool swapDRC);

  private:
	wxGLContext* m_glContext = nullptr;
	OpenGLCanvas* m_tvView = nullptr;
	OpenGLCanvas* m_padView = nullptr;
} s_glCanvasManager;

class OpenGLCanvas : public IRenderCanvas, public wxGLCanvas, public OpenGLRenderer::OpenGLCallbacks
{
public:
	OpenGLCanvas(wxWindow* parent, const wxSize& size, bool is_main_window)
		: IRenderCanvas(is_main_window), wxGLCanvas(parent, wxID_ANY, g_gl_attribute_list, wxDefaultPosition, size, wxFULL_REPAINT_ON_RESIZE | wxWANTS_CHARS)
	{
		if (m_is_main_window)
		{
<<<<<<< HEAD:src/gui/canvas/OpenGLCanvas.cpp
			sGLTVView = this;
			sGLContext = new wxGLContext(this);
=======
			s_glCanvasManager.SetTVView(this);
			s_glCanvasManager.SetGLContext(new wxGLContext(this));

>>>>>>> public/main:src/gui/wxgui/canvas/OpenGLCanvas.cpp
			g_renderer = std::make_unique<OpenGLRenderer>();
			OpenGLRenderer::GetInstance()->RegisterOpenGLCallbacks(this);
		}
		else
		{
			s_glCanvasManager.SetPadView(this);
		}

		wxWindow::EnableTouchEvents(wxTOUCH_PAN_GESTURES);
	}

	~OpenGLCanvas() override
	{
		// todo - if this is the main window, make sure the renderer has been shut down

		if (m_is_main_window)
			s_glCanvasManager.SetTVView(nullptr);
		else
			s_glCanvasManager.SetPadView(nullptr);

		s_glCanvasManager.DeleteGLContext();
	}

	bool GLCanvas_HasPadViewOpen() override;
	bool GLCanvas_MakeCurrent(bool padView) override;
	void GLCanvas_SwapBuffers(bool swapTV, bool swapDRC) override;

	void UpdateVSyncState()
	{
		int configValue = GetConfig().vsync.GetValue();
		if(m_activeVSyncState != configValue)
		{
#if BOOST_OS_WINDOWS
			if(wglSwapIntervalEXT)
				wglSwapIntervalEXT(configValue); // 1 = enabled, 0 = disabled
#elif BOOST_OS_LINUX
			if (eglSwapInterval)
			{
				if (eglSwapInterval(eglGetCurrentDisplay(), configValue) == EGL_FALSE)
				{
					cemuLog_log(LogType::Force, "Failed to set vsync using EGL");
				}
			}
#else
			cemuLog_log(LogType::Force, "OpenGL vsync not implemented");
#endif
			m_activeVSyncState = configValue;
		}
	}

private:
	int m_activeVSyncState = -1;
	//wxGLContext* m_context = nullptr;
};

wxWindow* GLCanvas_Create(wxWindow* parent, const wxSize& size, bool is_main_window)
{
	return new OpenGLCanvas(parent, size, is_main_window);
}

<<<<<<< HEAD:src/gui/canvas/OpenGLCanvas.cpp
bool OpenGLCanvas::GLCanvas_HasPadViewOpen()
=======
void GLCanvasManager::SwapBuffers(bool swapTV, bool swapDRC)
>>>>>>> public/main:src/gui/wxgui/canvas/OpenGLCanvas.cpp
{
	if (swapTV && m_tvView)
	{
		MakeCurrent(false);
		m_tvView->SwapBuffers();
		m_tvView->UpdateVSyncState();
	}

	if (swapDRC && m_padView)
	{
		MakeCurrent(true);
		m_padView->SwapBuffers();
		m_padView->UpdateVSyncState();
	}

	MakeCurrent(false);
}

<<<<<<< HEAD:src/gui/canvas/OpenGLCanvas.cpp
bool OpenGLCanvas::GLCanvas_MakeCurrent(bool padView)
=======
bool GLCanvasManager::MakeCurrent(bool padView)
>>>>>>> public/main:src/gui/wxgui/canvas/OpenGLCanvas.cpp
{
	OpenGLCanvas* canvas = padView ? m_padView : m_tvView;
	if (!canvas)
		return false;
	m_glContext->SetCurrent(*canvas);
	return true;
<<<<<<< HEAD:src/gui/canvas/OpenGLCanvas.cpp
}

void OpenGLCanvas::GLCanvas_SwapBuffers(bool swapTV, bool swapDRC)
{
	if (swapTV && sGLTVView)
	{
		GLCanvas_MakeCurrent(false);
		sGLTVView->SwapBuffers();
		sGLTVView->UpdateVSyncState();
	}
	if (swapDRC && sGLPadView)
	{
		GLCanvas_MakeCurrent(true);
		sGLPadView->SwapBuffers();
		sGLPadView->UpdateVSyncState();
	}

	GLCanvas_MakeCurrent(false);
}
=======
}
>>>>>>> public/main:src/gui/wxgui/canvas/OpenGLCanvas.cpp
